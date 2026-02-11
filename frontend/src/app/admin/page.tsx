"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api";
import {
  Film,
  Building2,
  Calendar,
  Ticket,
  DollarSign,
  TrendingUp,
  Users,
  Clock,
} from "lucide-react";

interface DashboardStats {
  totalMovies: number;
  activeMovies: number;
  totalTheaters: number;
  totalScreens: number;
  totalShows: number;
  showsToday: number;
}

interface BookingStats {
  totalBookings: number;
  confirmedBookings: number;
  pendingBookings: number;
  cancelledBookings: number;
  bookingsToday: number;
  revenueToday: number;
  revenueThisMonth: number;
  totalRevenue: number;
}

export default function AdminDashboard() {
  const [dashboardStats, setDashboardStats] = useState<DashboardStats | null>(
    null,
  );
  const [bookingStats, setBookingStats] = useState<BookingStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const [dashRes, bookRes] = await Promise.all([
          adminApi.getDashboardStats(),
          adminApi.getBookingStats(),
        ]);
        setDashboardStats(dashRes.data);
        setBookingStats(bookRes.data);
      } catch (err: any) {
        setError(err.message || "Failed to fetch stats");
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-rose-500"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 text-red-600 p-4 rounded-lg">
        Error: {error}
      </div>
    );
  }

  const statsCards = [
    {
      label: "Total Movies",
      value: dashboardStats?.totalMovies || 0,
      subtext: `${dashboardStats?.activeMovies || 0} active`,
      icon: Film,
      color: "bg-blue-500",
    },
    {
      label: "Theaters",
      value: dashboardStats?.totalTheaters || 0,
      subtext: `${dashboardStats?.totalScreens || 0} screens`,
      icon: Building2,
      color: "bg-purple-500",
    },
    {
      label: "Total Shows",
      value: dashboardStats?.totalShows || 0,
      subtext: `${dashboardStats?.showsToday || 0} today`,
      icon: Calendar,
      color: "bg-green-500",
    },
    {
      label: "Total Bookings",
      value: bookingStats?.totalBookings || 0,
      subtext: `${bookingStats?.bookingsToday || 0} today`,
      icon: Ticket,
      color: "bg-orange-500",
    },
    {
      label: "Revenue Today",
      value: `₹${(bookingStats?.revenueToday || 0).toLocaleString()}`,
      subtext: "Confirmed bookings",
      icon: DollarSign,
      color: "bg-emerald-500",
    },
    {
      label: "Revenue This Month",
      value: `₹${(bookingStats?.revenueThisMonth || 0).toLocaleString()}`,
      subtext: `Total: ₹${(bookingStats?.totalRevenue || 0).toLocaleString()}`,
      icon: TrendingUp,
      color: "bg-rose-500",
    },
  ];

  const bookingBreakdown = [
    {
      label: "Confirmed",
      value: bookingStats?.confirmedBookings || 0,
      color: "bg-green-500",
    },
    {
      label: "Pending",
      value: bookingStats?.pendingBookings || 0,
      color: "bg-yellow-500",
    },
    {
      label: "Cancelled",
      value: bookingStats?.cancelledBookings || 0,
      color: "bg-red-500",
    },
  ];

  return (
    <div>
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800">Dashboard</h1>
        <p className="text-gray-500">Overview of your BookMyShow platform</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        {statsCards.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div
              key={index}
              className="bg-white rounded-xl shadow-sm p-6 flex items-start gap-4"
            >
              <div className={`${stat.color} p-3 rounded-lg`}>
                <Icon className="text-white" size={24} />
              </div>
              <div>
                <p className="text-gray-500 text-sm">{stat.label}</p>
                <p className="text-2xl font-bold text-gray-800">{stat.value}</p>
                <p className="text-gray-400 text-xs">{stat.subtext}</p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Booking Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">
            Booking Status Breakdown
          </h2>
          <div className="space-y-4">
            {bookingBreakdown.map((item, index) => {
              const total = bookingStats?.totalBookings || 1;
              const percentage = Math.round((item.value / total) * 100) || 0;
              return (
                <div key={index}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-600">{item.label}</span>
                    <span className="font-medium">
                      {item.value} ({percentage}%)
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`${item.color} h-2 rounded-full transition-all`}
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">
            Quick Actions
          </h2>
          <div className="grid grid-cols-2 gap-4">
            <a
              href="/admin/movies"
              className="p-4 border rounded-lg hover:border-rose-500 hover:bg-rose-50 transition-colors"
            >
              <Film className="text-rose-500 mb-2" size={24} />
              <p className="font-medium">Manage Movies</p>
              <p className="text-sm text-gray-500">Add or edit movies</p>
            </a>
            <a
              href="/admin/shows"
              className="p-4 border rounded-lg hover:border-rose-500 hover:bg-rose-50 transition-colors"
            >
              <Calendar className="text-rose-500 mb-2" size={24} />
              <p className="font-medium">Schedule Shows</p>
              <p className="text-sm text-gray-500">Create new shows</p>
            </a>
            <a
              href="/admin/bookings"
              className="p-4 border rounded-lg hover:border-rose-500 hover:bg-rose-50 transition-colors"
            >
              <Ticket className="text-rose-500 mb-2" size={24} />
              <p className="font-medium">View Bookings</p>
              <p className="text-sm text-gray-500">Manage bookings</p>
            </a>
            <a
              href="/admin/cache"
              className="p-4 border rounded-lg hover:border-rose-500 hover:bg-rose-50 transition-colors"
            >
              <Clock className="text-rose-500 mb-2" size={24} />
              <p className="font-medium">Cache Control</p>
              <p className="text-sm text-gray-500">Clear caches</p>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
