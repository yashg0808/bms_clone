"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

/**
 * Payment page is no longer used.
 * Booking confirmation with guest details happens inline on the seat selection page.
 * Redirect to home if someone navigates here directly.
 */
export default function PaymentPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/");
  }, [router]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <p className="text-gray-500">Redirecting...</p>
    </div>
  );
}
