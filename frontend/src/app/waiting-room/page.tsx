"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Clock, Users, RefreshCw } from "lucide-react";

/**
 * Virtual Waiting Room page.
 * Users are redirected here when the system is under surge load.
 * Auto-retries every 10 seconds to check if capacity is available.
 */
export default function WaitingRoomPage() {
  const router = useRouter();
  const [countdown, setCountdown] = useState(10);
  const [retryCount, setRetryCount] = useState(0);
  const [checking, setChecking] = useState(false);

  useEffect(() => {
    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          handleRetry();
          return 10;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  async function handleRetry() {
    setChecking(true);
    setRetryCount((prev) => prev + 1);
    try {
      // Try to hit the health endpoint — if we don't get 302'd again, we're in
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/actuator/health`,
        { redirect: "manual" },
      );

      if (response.status === 200 || response.type === "opaqueredirect") {
        // We got through! Redirect back to the home page
        router.push("/");
        return;
      }
    } catch {
      // Still in waiting room
    } finally {
      setChecking(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 px-4 text-white">
      {/* Animated pulse background */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 h-80 w-80 animate-pulse rounded-full bg-purple-500/20 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 h-80 w-80 animate-pulse rounded-full bg-blue-500/20 blur-3xl" />
      </div>

      <div className="relative z-10 max-w-md text-center">
        {/* Icon */}
        <div className="mx-auto mb-8 flex h-24 w-24 items-center justify-center rounded-full bg-white/10 backdrop-blur-sm">
          <Users className="h-12 w-12 text-purple-300" />
        </div>

        {/* Title */}
        <h1 className="mb-3 text-3xl font-bold">You&apos;re in the Queue</h1>
        <p className="mb-8 text-lg text-gray-300">
          We&apos;re experiencing high demand right now. You&apos;ll be
          automatically let in once capacity is available.
        </p>

        {/* Countdown ring */}
        <div className="mx-auto mb-8 flex h-32 w-32 flex-col items-center justify-center rounded-full border-4 border-purple-400/50 bg-white/5">
          <Clock className="mb-1 h-5 w-5 text-purple-300" />
          <span className="text-4xl font-bold tabular-nums">{countdown}</span>
          <span className="text-xs text-gray-400">seconds</span>
        </div>

        {/* Status */}
        <div className="mb-6 rounded-lg bg-white/10 p-4 backdrop-blur-sm">
          <div className="flex items-center justify-center gap-2 text-sm">
            {checking ? (
              <>
                <RefreshCw className="h-4 w-4 animate-spin text-purple-300" />
                <span>Checking availability...</span>
              </>
            ) : (
              <>
                <div className="h-2 w-2 animate-pulse rounded-full bg-yellow-400" />
                <span>Waiting for capacity • Attempt #{retryCount + 1}</span>
              </>
            )}
          </div>
        </div>

        {/* Manual retry */}
        <button
          onClick={() => {
            setCountdown(10);
            handleRetry();
          }}
          disabled={checking}
          className="rounded-lg bg-purple-600 px-6 py-3 text-sm font-semibold transition-colors hover:bg-purple-500 disabled:opacity-50"
        >
          Try Now
        </button>

        <p className="mt-6 text-xs text-gray-500">
          This page auto-refreshes every 10 seconds. Please don&apos;t close
          this tab.
        </p>
      </div>
    </div>
  );
}
