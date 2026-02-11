"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api";
import { Search, Filter, X, RefreshCw } from "lucide-react";

interface Booking {
  id: string;
  bookingNumber: string;
  showId: string;
  guestName: string;
  guestEmail: string;
  guestPhone: string;
  status: string;
  totalAmount: number;
  finalAmount: number;
  seats: { seatLabel: string; seatType: string; price: number }[];
  createdAt: string;
}

interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const statusColors: Record<string, string> = {
  CONFIRMED: "bg-green-100 text-green-700",
  PENDING: "bg-yellow-100 text-yellow-700",
  CANCELLED: "bg-red-100 text-red-700",
  EXPIRED: "bg-gray-100 text-gray-700",
};

export default function AdminBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      const response = await adminApi.getBookings({
        page,
        size: 15,
        status: statusFilter || undefined,
        search: searchQuery || undefined,
      });
      const data: PagedResponse<Booking> = response.data;
      setBookings(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (error) {
      console.error("Failed to fetch bookings:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, [page, statusFilter]);

  const handleSearch = () => {
    setPage(0);
    fetchBookings();
  };

  const handleCancelBooking = async (bookingId: string) => {
    if (!confirm("Are you sure you want to cancel this booking?")) return;
    try {
      await adminApi.cancelBooking(bookingId, "Cancelled by admin");
      fetchBookings();
      setSelectedBooking(null);
    } catch (error) {
      console.error("Failed to cancel booking:", error);
      alert("Failed to cancel booking");
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Bookings</h1>
          <p className="text-gray-500">{totalElements} total bookings</p>
        </div>
        <button
          onClick={fetchBookings}
          className="flex items-center gap-2 px-4 py-2 border rounded-lg hover:bg-gray-50"
        >
          <RefreshCw size={18} />
          Refresh
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm p-4 mb-6">
        <div className="flex gap-4">
          <div className="flex-1 relative">
            <Search
              className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
              size={20}
            />
            <input
              type="text"
              placeholder="Search by name, email, phone, or booking number..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
          >
            <option value="">All Statuses</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="PENDING">Pending</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="EXPIRED">Expired</option>
          </select>
          <button
            onClick={handleSearch}
            className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600"
          >
            Search
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500"></div>
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Booking #
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Guest
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Seats
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Date
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {bookings.map((booking) => (
                <tr key={booking.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <span className="font-mono text-sm font-medium">
                      {booking.bookingNumber}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div>
                      <p className="font-medium text-gray-900">
                        {booking.guestName}
                      </p>
                      <p className="text-sm text-gray-500">
                        {booking.guestEmail}
                      </p>
                      <p className="text-sm text-gray-500">
                        {booking.guestPhone}
                      </p>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-600">
                    {booking.seats?.map((s) => s.seatLabel).join(", ") || "-"}
                  </td>
                  <td className="px-6 py-4">
                    <span className="font-medium">₹{booking.finalAmount}</span>
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`px-2 py-1 rounded-full text-xs ${
                        statusColors[booking.status] || "bg-gray-100"
                      }`}
                    >
                      {booking.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {new Date(booking.createdAt).toLocaleDateString()}{" "}
                    {new Date(booking.createdAt).toLocaleTimeString()}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => setSelectedBooking(booking)}
                      className="text-rose-500 hover:text-rose-600 text-sm font-medium"
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
              {bookings.length === 0 && (
                <tr>
                  <td
                    colSpan={7}
                    className="px-6 py-12 text-center text-gray-500"
                  >
                    No bookings found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}

        {/* Pagination */}
        <div className="px-6 py-4 border-t flex items-center justify-between">
          <p className="text-sm text-gray-500">
            Page {page + 1} of {totalPages || 1}
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              className="px-3 py-1 border rounded hover:bg-gray-50 disabled:opacity-50"
            >
              Previous
            </button>
            <button
              onClick={() => setPage(page + 1)}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 border rounded hover:bg-gray-50 disabled:opacity-50"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedBooking && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl w-full max-w-lg">
            <div className="flex items-center justify-between p-4 border-b">
              <h2 className="text-xl font-semibold">Booking Details</h2>
              <button
                onClick={() => setSelectedBooking(null)}
                className="p-2 hover:bg-gray-100 rounded-lg"
              >
                <X size={20} />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-500">Booking Number</p>
                  <p className="font-mono font-medium">
                    {selectedBooking.bookingNumber}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Status</p>
                  <span
                    className={`inline-block px-2 py-1 rounded-full text-xs ${
                      statusColors[selectedBooking.status]
                    }`}
                  >
                    {selectedBooking.status}
                  </span>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Guest Name</p>
                  <p className="font-medium">{selectedBooking.guestName}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Phone</p>
                  <p>{selectedBooking.guestPhone}</p>
                </div>
                <div className="col-span-2">
                  <p className="text-sm text-gray-500">Email</p>
                  <p>{selectedBooking.guestEmail}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Total Amount</p>
                  <p className="font-medium">₹{selectedBooking.totalAmount}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">Final Amount</p>
                  <p className="font-bold text-lg">
                    ₹{selectedBooking.finalAmount}
                  </p>
                </div>
              </div>

              <div>
                <p className="text-sm text-gray-500 mb-2">Seats</p>
                <div className="flex flex-wrap gap-2">
                  {selectedBooking.seats?.map((seat, i) => (
                    <span
                      key={i}
                      className="px-3 py-1 bg-gray-100 rounded-lg text-sm"
                    >
                      {seat.seatLabel} ({seat.seatType}) - ₹{seat.price}
                    </span>
                  ))}
                </div>
              </div>

              <div>
                <p className="text-sm text-gray-500">Booked On</p>
                <p>{new Date(selectedBooking.createdAt).toLocaleString()}</p>
              </div>
            </div>

            <div className="flex justify-end gap-3 p-4 border-t">
              {selectedBooking.status === "CONFIRMED" && (
                <button
                  onClick={() => handleCancelBooking(selectedBooking.id)}
                  className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600"
                >
                  Cancel Booking
                </button>
              )}
              <button
                onClick={() => setSelectedBooking(null)}
                className="px-4 py-2 border rounded-lg hover:bg-gray-50"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
