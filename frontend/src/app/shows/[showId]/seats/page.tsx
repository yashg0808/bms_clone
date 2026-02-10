"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { showApi, bookingApi } from "@/lib/api";
import { ShowSeat } from "@/types";
import { useBookingStore } from "@/store";
import SeatMap from "@/components/booking/SeatMap";
import toast from "react-hot-toast";

export default function SeatSelectionPage() {
  const params = useParams();
  const router = useRouter();
  const showId = params.showId as string;
  const { selectedSeats, clearSeats, setLockInfo } = useBookingStore();
  const [seats, setSeats] = useState<ShowSeat[]>([]);
  const [loading, setLoading] = useState(true);
  const [locking, setLocking] = useState(false);

  useEffect(() => {
    fetchSeats();
    return () => clearSeats();
  }, [showId]);

  async function fetchSeats() {
    try {
      const res = await showApi.getShowSeats(showId);
      setSeats(res.data?.data || []);
    } catch (error) {
      toast.error("Failed to load seats");
    } finally {
      setLoading(false);
    }
  }

  async function handleProceed() {
    if (selectedSeats.length === 0) {
      toast.error("Please select at least one seat");
      return;
    }

    setLocking(true);
    try {
      const res = await bookingApi.lockSeats({
        showId,
        seatIds: selectedSeats,
      });

      const data = res.data?.data;
      setLockInfo(data.lockToken, data.bookingId || "", data.expiresAt);

      toast.success("Seats locked! Complete payment within 8 minutes.");
      router.push("/payment");
    } catch (error: any) {
      const message = error.response?.data?.message || "Failed to lock seats";
      toast.error(message);
      // Refresh seats to show updated availability
      fetchSeats();
    } finally {
      setLocking(false);
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-2 text-2xl font-bold">Select Your Seats</h1>
      <p className="mb-8 text-sm text-gray-500">
        You can select up to 10 seats. Locked seats will be held for 8 minutes.
      </p>

      <SeatMap seats={seats} onProceed={handleProceed} />

      {locking && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="rounded-lg bg-white p-8 text-center">
            <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-primary-500 border-t-transparent" />
            <p className="font-medium">Locking your seats...</p>
            <p className="text-sm text-gray-500">Please wait</p>
          </div>
        </div>
      )}
    </div>
  );
}
