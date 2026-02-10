"use client";

import { ShowSeat } from "@/types";
import { cn, formatCurrency } from "@/lib/utils";
import { useBookingStore } from "@/store";

interface SeatMapProps {
  seats: ShowSeat[];
  onProceed: () => void;
}

type SeatTypeSection = {
  type: string;
  label: string;
  price: number;
  rows: Record<string, ShowSeat[]>;
};

export default function SeatMap({ seats, onProceed }: SeatMapProps) {
  const { selectedSeats, toggleSeat } = useBookingStore();

  // Group seats by seatType, then by row
  const sections = buildSections(seats);

  const totalAmount = seats
    .filter((s) => selectedSeats.includes(s.id))
    .reduce((sum, s) => sum + s.price, 0);

  const selectedCount = selectedSeats.length;

  const getSeatColor = (seat: ShowSeat) => {
    if (selectedSeats.includes(seat.id))
      return "bg-emerald-500 text-white border-emerald-600 shadow-md scale-110";
    if (seat.status === "BOOKED")
      return "bg-gray-300 text-gray-500 border-gray-300 cursor-not-allowed";
    if (seat.status === "LOCKED")
      return "bg-amber-400 text-amber-900 border-amber-500 cursor-not-allowed";
    return "bg-white border-gray-300 text-gray-600 hover:border-emerald-400 hover:bg-emerald-50 cursor-pointer";
  };

  const seatTypeStyles: Record<string, string> = {
    RECLINER: "rounded-t-xl",
    PREMIUM: "rounded-lg",
    REGULAR: "rounded",
  };

  return (
    <div className="mx-auto max-w-5xl px-4">
      {/* Screen */}
      <div className="mb-10 text-center">
        <div className="relative mx-auto w-3/4">
          <div className="h-3 rounded-t-[50%] bg-gradient-to-b from-gray-300 to-gray-200 shadow-[0_0_15px_rgba(0,0,0,0.1)]" />
          <div className="absolute -bottom-1 left-0 right-0 h-1 bg-gradient-to-r from-transparent via-gray-200 to-transparent" />
        </div>
        <p className="mt-3 text-[11px] font-semibold uppercase tracking-[0.2em] text-gray-400">
          All eyes this way please!
        </p>
      </div>

      {/* Seat sections by type */}
      <div className="space-y-8">
        {sections.map((section) => (
          <div key={section.type}>
            {/* Section header */}
            <div className="mb-3 flex items-center gap-3">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-500">
                {section.label}
              </span>
              <span className="text-xs font-medium text-emerald-600">
                {formatCurrency(section.price)}
              </span>
              <div className="h-px flex-1 bg-gray-200" />
            </div>

            {/* Rows in this section */}
            <div className="space-y-1.5">
              {Object.entries(section.rows)
                .sort(([a], [b]) => a.localeCompare(b))
                .map(([row, rowSeats]) => {
                  const sorted = rowSeats.sort(
                    (a, b) => a.columnNumber - b.columnNumber
                  );
                  const maxCol = Math.max(...sorted.map((s) => s.columnNumber));
                  // Insert aisle gaps after ~1/3 and ~2/3 of the row
                  const gap1 = Math.floor(maxCol / 3);
                  const gap2 = Math.floor((2 * maxCol) / 3);

                  return (
                    <div
                      key={row}
                      className="flex items-center justify-center gap-0.5"
                    >
                      {/* Row label */}
                      <span className="w-7 text-right text-[11px] font-semibold text-gray-400 mr-2">
                        {row}
                      </span>

                      <div className="flex items-center gap-0.5">
                        {sorted.map((seat) => (
                          <div key={seat.id} className="flex items-center">
                            {/* Aisle gap */}
                            {(seat.columnNumber === gap1 + 1 ||
                              seat.columnNumber === gap2 + 1) && (
                              <div className="w-4" />
                            )}
                            <button
                              onClick={() => {
                                if (seat.status === "AVAILABLE") {
                                  toggleSeat(seat.id);
                                }
                              }}
                              disabled={seat.status !== "AVAILABLE"}
                              className={cn(
                                "flex items-center justify-center border text-[10px] font-medium transition-all duration-150",
                                section.type === "RECLINER"
                                  ? "h-9 w-9"
                                  : "h-7 w-7",
                                seatTypeStyles[section.type] || "rounded",
                                getSeatColor(seat)
                              )}
                              title={`${seat.seatNumber} · ${section.label} · ${formatCurrency(seat.price)}`}
                            >
                              {seat.columnNumber}
                            </button>
                          </div>
                        ))}
                      </div>

                      {/* Row label (right side) */}
                      <span className="w-7 text-left text-[11px] font-semibold text-gray-400 ml-2">
                        {row}
                      </span>
                    </div>
                  );
                })}
            </div>
          </div>
        ))}
      </div>

      {/* Legend */}
      <div className="mt-10 flex flex-wrap justify-center gap-6">
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded border border-gray-300 bg-white" />
          <span className="text-xs text-gray-500">Available</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded border border-emerald-600 bg-emerald-500" />
          <span className="text-xs text-gray-500">Selected</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded bg-gray-300" />
          <span className="text-xs text-gray-500">Sold</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-5 w-5 rounded bg-amber-400" />
          <span className="text-xs text-gray-500">Locked</span>
        </div>
      </div>

      {/* Selected seats summary */}
      {selectedCount > 0 && (
        <div className="sticky bottom-4 mt-8 rounded-xl border bg-white p-5 shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">
                {selectedCount} Seat{selectedCount > 1 ? "s" : ""} selected
              </p>
              <p className="text-xl font-bold text-gray-900">
                {formatCurrency(totalAmount)}
              </p>
            </div>
            <button
              onClick={onProceed}
              className="rounded-lg bg-rose-500 px-8 py-3 text-sm font-semibold text-white shadow-md hover:bg-rose-600 transition-colors"
            >
              Lock Seats
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Build sections grouped by seat type, ordered: RECLINER → PREMIUM → REGULAR
 */
function buildSections(seats: ShowSeat[]): SeatTypeSection[] {
  const typeOrder = ["RECLINER", "PREMIUM", "REGULAR"];
  const typeLabels: Record<string, string> = {
    RECLINER: "Recliner",
    PREMIUM: "Premium",
    REGULAR: "Regular",
  };

  const grouped: Record<string, ShowSeat[]> = {};
  for (const seat of seats) {
    const type = seat.seatType || "REGULAR";
    if (!grouped[type]) grouped[type] = [];
    grouped[type].push(seat);
  }

  const sections: SeatTypeSection[] = [];
  for (const type of typeOrder) {
    const seatsOfType = grouped[type];
    if (!seatsOfType || seatsOfType.length === 0) continue;

    // Group by row
    const rows: Record<string, ShowSeat[]> = {};
    for (const seat of seatsOfType) {
      const row = seat.seatRow || "?";
      if (!rows[row]) rows[row] = [];
      rows[row].push(seat);
    }

    // Average price for the section header
    const avgPrice =
      seatsOfType.reduce((sum, s) => sum + s.price, 0) / seatsOfType.length;

    sections.push({
      type,
      label: typeLabels[type] || type,
      price: avgPrice,
      rows,
    });
  }

  return sections;
}
