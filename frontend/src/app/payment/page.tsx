"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { paymentApi, bookingApi } from "@/lib/api";
import { useBookingStore } from "@/store";
import { generateIdempotencyKey, formatCurrency } from "@/lib/utils";
import toast from "react-hot-toast";
import { CreditCard, Clock, Shield, CheckCircle } from "lucide-react";

type PaymentMethod = "RAZORPAY" | "STRIPE" | "UPI" | "CARD";

export default function PaymentPage() {
  const router = useRouter();
  const { lockToken, bookingId, expiresAt, clearSeats } = useBookingStore();
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("RAZORPAY");
  const [processing, setProcessing] = useState(false);
  const [timeLeft, setTimeLeft] = useState<number>(0);
  const [success, setSuccess] = useState(false);
  const [bookingDetails, setBookingDetails] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [booking, setBooking] = useState<any>(null);

  useEffect(() => {
    if (!lockToken || !bookingId) {
      toast.error("No active booking found");
      router.push("/");
      return;
    }

    // Fetch booking details to get amount
    async function fetchBooking() {
      if (!bookingId) return;

      try {
        const res = await bookingApi.getBooking(bookingId);
        setBooking(res.data?.data);
      } catch (error) {
        toast.error("Failed to load booking details");
        router.push("/");
      } finally {
        setLoading(false);
      }
    }

    fetchBooking();
  }, [lockToken, bookingId]);

  // Countdown timer
  useEffect(() => {
    if (!expiresAt || success) return;

    const updateTimer = () => {
      const remaining = Math.max(
        0,
        Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000),
      );
      setTimeLeft(remaining);
      if (remaining === 0) {
        toast.error("Booking expired! Seats have been released.");
        clearSeats();
        router.push("/");
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [expiresAt, success]);

  const formatTime = useCallback((seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, "0")}`;
  }, []);

  async function handlePayment() {
    if (processing) return;

    if (!bookingId || !booking) {
      toast.error("No active booking found");
      router.push("/");
      return;
    }

    setProcessing(true);

    try {
      const idempotencyKey = generateIdempotencyKey();

      // Step 1: Initiate payment
      const paymentRes = await paymentApi.initiatePayment({
        bookingId,
        amount: booking.finalAmount,
        paymentMethod,
        idempotencyKey,
      });

      const payment = paymentRes.data?.data;

      // Step 2: Simulate payment gateway interaction
      // In production, this would open Razorpay/Stripe checkout
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // Step 3: Verify payment
      await paymentApi.verifyPayment({
        paymentId: payment.id,
        gatewayPaymentId: payment.gatewayOrderId || "sim_pay_" + Date.now(),
        gatewaySignature: "simulated_signature_" + Date.now(),
      });

      // Step 4: Confirm booking
      await bookingApi.confirmBooking({
        bookingId,
        lockToken: lockToken || "",
      });

      setSuccess(true);
      setBookingDetails({ bookingId, paymentId: payment.id });
      toast.success("Booking confirmed!");
      clearSeats();
    } catch (error: any) {
      const message =
        error.response?.data?.message || "Payment failed. Please try again.";
      toast.error(message);
    } finally {
      setProcessing(false);
    }
  }

  if (success && bookingDetails) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-green-100">
          <CheckCircle className="h-10 w-10 text-green-600" />
        </div>
        <h1 className="mb-2 text-2xl font-bold">Booking Confirmed!</h1>
        <p className="mb-6 text-gray-500">
          Your tickets have been booked successfully.
        </p>
        <div className="mb-8 rounded-lg bg-gray-50 p-4 text-left text-sm">
          <div className="mb-2 flex justify-between">
            <span className="text-gray-500">Booking ID</span>
            <span className="font-mono font-medium">
              {bookingDetails.bookingId.substring(0, 8)}...
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Payment ID</span>
            <span className="font-mono font-medium">
              {bookingDetails.paymentId.substring(0, 8)}...
            </span>
          </div>
        </div>
        <div className="space-y-3">
          <button
            onClick={() => router.push(`/bookings/${bookingDetails.bookingId}`)}
            className="w-full rounded-lg bg-primary-500 py-3 font-semibold text-white hover:bg-primary-600"
          >
            View Booking
          </button>
          <button
            onClick={() => router.push("/")}
            className="w-full rounded-lg border py-3 font-semibold text-gray-700 hover:bg-gray-50"
          >
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  if (!booking) {
    return null;
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      {/* Timer Bar */}
      <div
        className={`mb-6 flex items-center justify-between rounded-lg p-4 ${
          timeLeft <= 60
            ? "bg-red-50 text-red-700"
            : "bg-amber-50 text-amber-700"
        }`}
      >
        <div className="flex items-center gap-2">
          <Clock className="h-5 w-5" />
          <span className="font-medium">Complete payment in</span>
        </div>
        <span className="text-xl font-bold font-mono">
          {formatTime(timeLeft)}
        </span>
      </div>

      <h1 className="mb-6 text-2xl font-bold">Payment</h1>

      {/* Payment Method Selection */}
      <div className="mb-6 rounded-lg border p-6">
        <h2 className="mb-4 text-lg font-semibold">Select Payment Method</h2>
        <div className="grid grid-cols-2 gap-3">
          {[
            {
              value: "RAZORPAY" as const,
              label: "Razorpay",
              desc: "UPI, Cards, Wallets",
            },
            {
              value: "STRIPE" as const,
              label: "Stripe",
              desc: "International Cards",
            },
            {
              value: "UPI" as const,
              label: "UPI",
              desc: "Google Pay, PhonePe",
            },
            {
              value: "CARD" as const,
              label: "Card",
              desc: "Debit / Credit Card",
            },
          ].map((method) => (
            <button
              key={method.value}
              onClick={() => setPaymentMethod(method.value)}
              className={`rounded-lg border-2 p-4 text-left transition-colors ${
                paymentMethod === method.value
                  ? "border-primary-500 bg-primary-50"
                  : "border-gray-200 hover:border-gray-300"
              }`}
            >
              <div className="flex items-center gap-3">
                <CreditCard className="h-5 w-5 text-gray-600" />
                <div>
                  <p className="font-semibold">{method.label}</p>
                  <p className="text-xs text-gray-500">{method.desc}</p>
                </div>
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Security Notice */}
      <div className="mb-6 flex items-start gap-3 rounded-lg bg-gray-50 p-4 text-sm text-gray-600">
        <Shield className="mt-0.5 h-5 w-5 flex-shrink-0 text-green-600" />
        <div>
          <p className="font-medium text-gray-700">Secure Payment</p>
          <p>
            Your payment is processed securely. We do not store your card
            details. All transactions are encrypted.
          </p>
        </div>
      </div>

      {/* Pay Button */}
      <button
        onClick={handlePayment}
        disabled={processing || timeLeft === 0}
        className="w-full rounded-lg bg-primary-500 py-4 text-lg font-bold text-white transition-colors hover:bg-primary-600 disabled:cursor-not-allowed disabled:bg-gray-300"
      >
        {processing ? (
          <span className="flex items-center justify-center gap-2">
            <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
            Processing...
          </span>
        ) : (
          "Pay Now"
        )}
      </button>

      <p className="mt-4 text-center text-xs text-gray-500">
        By proceeding, you agree to our Terms of Service and Refund Policy.
      </p>
    </div>
  );
}
