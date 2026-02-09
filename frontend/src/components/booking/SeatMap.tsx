"use client";

import { ShowSeat } from "@/types";
import { cn, formatCurrency } from "@/lib/utils";
import { useBookingStore } from "@/store";

interface SeatMapProps {
  seats: ShowSeat[];
  onProceed: () => void;
}

export default function SeatMap({ seats, onProceed }: SeatMapProps) {
  const { selectedSeats, toggleSeat } = useBookingStore();

  // Group seats by row
  const seatsByRow = seats.reduce(
    (acc, seat) => {
      const row = seat.seatRow || "A";
      if (!acc[row]) acc[row] = [];
      acc[row].push(seat);
      return acc;
    },
    {} as Record<string, ShowSeat[]>
  );

  const totalAmount = seats
    .filter((s) => selectedSeats.includes(s.id))
    .reduce((sum, s) => sum + s.price, 0);

  const getSeatColor = (seat: ShowSeat) => {
    if (selectedSeats.includes(seat.id)) return "bg-green-500 text-white";
    if (seat.status === "BOOKED") return "bg-gray-400 text-gray-600 cursor-not-allowed";
    if (seat.status === "LOCKED") return "bg-yellow-400 text-yellow-800 cursor-not-allowed";
    return "bg-white border-2 border-green-500 text-green-700 hover:bg-green-100 cursor-pointer";
  };

  return (
    <div className="mx-auto max-w-4xl">
      {/* Screen indicator */}
      <div className="mb-8 text-center">
        <div className="mx-auto mb-2 h-2 w-3/4 rounded-t-full bg-gray-300" />
        <p className="text-xs text-gray-500">SCREEN THIS WAY</p>
      </div>

      {/* Seat grid */}
      <div className="space-y-2">
        {Object.entries(seatsByRow)
          .sort(([a], [b]) => a.localeCompare(b))
          .map(([row, rowSeats]) => (
            <div key={row} className="flex items-center justify-center gap-1">
              <span className="w-6 text-center text-xs font-medium text-gray-500">
                {row}
              </span>
              <div className="flex gap-1">
                {rowSeats
                  .sort((a, b) => (a.seatNumber || "").localeCompare(b.seatNumber || ""))
                  .map((seat) => (
                    <button
                      key={seat.id}
                      onClick={() => {
                        if (seat.status === "AVAILABLE") {
                          toggleSeat(seat.id);
                        }
                      }}
                      disabled={seat.status !== "AVAILABLE"}
                      className={cn(
                        "flex h-7 w-7 items-center justify-center rounded text-[10px] font-medium transition-colors",
                        getSeatColor(seat)
                      )}
                      title={`${row}${seat.seatNumber} - ${formatCurrency(seat.price)}`}
                    >
                      {seat.seatNumber}
                    </button>
                  ))}
              </div>
            </div>
          ))}
      </div>

      {/* Legend */}
      <div className="mt-8 flex justify-center gap-6">
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded border-2 border-green-500 bg-white" />
          <span className="text-xs text-gray-600">Available</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded bg-green-500" />
          <span className="text-xs text-gray-600">Selected</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded bg-gray-400" />
          <span className="text-xs text-gray-600">Booked</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded bg-yellow-400" />
          <span className="text-xs text-gray-600">Locked</span>
        </div>
      </div>

      {/* Selected seats summary */}
      {selectedSeats.length > 0 && (
        <div className="mt-8 rounded-lg border bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">
                {selectedSeats.length} Seat(s) Selected
              </p>
              <p className="text-lg font-bold text-gray-900">
                Total: {formatCurrency(totalAmount)}
              </p>
            </div>
            <button
              onClick={onProceed}
              className="rounded-lg bg-primary-500 px-6 py-3 font-medium text-white hover:bg-primary-600"
            >
              Proceed to Lock Seats
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
