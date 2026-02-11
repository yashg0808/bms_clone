"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api";
import { Plus, Search, Calendar, Clock, X } from "lucide-react";

interface Show {
  id: string;
  movieId: string;
  movieTitle: string;
  screenId: string;
  screenName: string;
  screenType: string;
  theaterName: string;
  theaterId: string;
  showDate: string;
  startTime: string;
  endTime: string;
  basePrice: number;
  premiumPrice: number;
  reclinerPrice: number;
}

interface Movie {
  id: string;
  title: string;
}

interface Theater {
  id: string;
  name: string;
  cityName: string;
}

interface Screen {
  id: string;
  name: string;
  screenType: string;
  totalSeats: number;
}

interface PagedResponse<T> {
  content: T[];
  page: number;
  totalPages: number;
  totalElements: number;
}

export default function AdminShowsPage() {
  const [shows, setShows] = useState<Show[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [dateFilter, setDateFilter] = useState(
    new Date().toISOString().split("T")[0],
  );

  const fetchShows = async () => {
    try {
      setLoading(true);
      const response = await adminApi.getShows({
        page,
        size: 15,
        date: dateFilter,
      });
      const data: PagedResponse<Show> = response.data;
      setShows(data.content);
      setTotalPages(data.totalPages);
    } catch (error) {
      console.error("Failed to fetch shows:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShows();
  }, [page, dateFilter]);

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to cancel this show?")) return;
    try {
      await adminApi.deleteShow(id);
      fetchShows();
    } catch (error) {
      console.error("Failed to delete show:", error);
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Shows</h1>
          <p className="text-gray-500">Schedule and manage show timings</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 bg-rose-500 text-white px-4 py-2 rounded-lg hover:bg-rose-600"
        >
          <Plus size={20} />
          Schedule Show
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm p-4 mb-6">
        <div className="flex gap-4 items-center">
          <div className="flex items-center gap-2">
            <Calendar size={20} className="text-gray-400" />
            <input
              type="date"
              value={dateFilter}
              onChange={(e) => {
                setDateFilter(e.target.value);
                setPage(0);
              }}
              className="px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
          </div>
          <button
            onClick={() =>
              setDateFilter(new Date().toISOString().split("T")[0])
            }
            className="px-3 py-2 text-sm text-rose-500 hover:bg-rose-50 rounded-lg"
          >
            Today
          </button>
          <button
            onClick={() => {
              const tomorrow = new Date();
              tomorrow.setDate(tomorrow.getDate() + 1);
              setDateFilter(tomorrow.toISOString().split("T")[0]);
            }}
            className="px-3 py-2 text-sm text-rose-500 hover:bg-rose-50 rounded-lg"
          >
            Tomorrow
          </button>
        </div>
      </div>

      {/* Shows Grid */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500"></div>
          </div>
        ) : shows.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            No shows scheduled for {dateFilter}
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Movie
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Theater / Screen
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Time
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Prices
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {shows.map((show) => (
                <tr key={show.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <p className="font-medium text-gray-900">
                      {show.movieTitle}
                    </p>
                    <p className="text-sm text-gray-500">{show.showDate}</p>
                  </td>
                  <td className="px-6 py-4">
                    <p className="font-medium">{show.theaterName}</p>
                    <p className="text-sm text-gray-500">
                      {show.screenName} ({show.screenType})
                    </p>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <Clock size={16} className="text-gray-400" />
                      <span>
                        {show.startTime} - {show.endTime}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm">
                    <p>Base: ₹{show.basePrice}</p>
                    {show.premiumPrice && (
                      <p className="text-gray-500">
                        Premium: ₹{show.premiumPrice}
                      </p>
                    )}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => handleDelete(show.id)}
                      className="text-red-500 hover:text-red-600 text-sm"
                    >
                      Cancel
                    </button>
                  </td>
                </tr>
              ))}
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

      {/* Create Modal */}
      {showModal && (
        <CreateShowModal
          onClose={() => setShowModal(false)}
          onSave={() => {
            setShowModal(false);
            fetchShows();
          }}
        />
      )}
    </div>
  );
}

function CreateShowModal({
  onClose,
  onSave,
}: {
  onClose: () => void;
  onSave: () => void;
}) {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [theaters, setTheaters] = useState<Theater[]>([]);
  const [screens, setScreens] = useState<Screen[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [formData, setFormData] = useState({
    movieId: "",
    theaterId: "",
    screenId: "",
    showDate: new Date().toISOString().split("T")[0],
    startTime: "10:00",
    endTime: "12:30",
    basePrice: 150,
    premiumPrice: 250,
    reclinerPrice: 350,
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [moviesRes, theatersRes] = await Promise.all([
          adminApi.getMovies({ page: 0, size: 100 }),
          adminApi.getTheaters(),
        ]);
        setMovies(moviesRes.data.content);
        setTheaters(theatersRes.data);
      } catch (error) {
        console.error("Failed to fetch data:", error);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  useEffect(() => {
    if (formData.theaterId) {
      adminApi.getScreens(formData.theaterId).then((res) => {
        setScreens(res.data);
      });
    }
  }, [formData.theaterId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await adminApi.createShow({
        movieId: formData.movieId,
        screenId: formData.screenId,
        showDate: formData.showDate,
        startTime: formData.startTime + ":00",
        endTime: formData.endTime + ":00",
        basePrice: formData.basePrice,
        premiumPrice: formData.premiumPrice,
        reclinerPrice: formData.reclinerPrice,
      });
      onSave();
    } catch (error: any) {
      console.error("Failed to create show:", error);
      alert(error.response?.data?.message || "Failed to create show");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div className="bg-white rounded-xl p-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500 mx-auto"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl w-full max-w-lg">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-xl font-semibold">Schedule New Show</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Movie *
            </label>
            <select
              required
              value={formData.movieId}
              onChange={(e) =>
                setFormData({ ...formData, movieId: e.target.value })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              <option value="">Select a movie</option>
              {movies.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.title}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Theater *
            </label>
            <select
              required
              value={formData.theaterId}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  theaterId: e.target.value,
                  screenId: "",
                })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              <option value="">Select a theater</option>
              {theaters.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} - {t.cityName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Screen *
            </label>
            <select
              required
              value={formData.screenId}
              onChange={(e) =>
                setFormData({ ...formData, screenId: e.target.value })
              }
              disabled={!formData.theaterId}
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 disabled:bg-gray-100"
            >
              <option value="">Select a screen</option>
              {screens.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.screenType}) - {s.totalSeats} seats
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Date *
              </label>
              <input
                type="date"
                required
                value={formData.showDate}
                onChange={(e) =>
                  setFormData({ ...formData, showDate: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Start Time *
              </label>
              <input
                type="time"
                required
                value={formData.startTime}
                onChange={(e) =>
                  setFormData({ ...formData, startTime: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                End Time *
              </label>
              <input
                type="time"
                required
                value={formData.endTime}
                onChange={(e) =>
                  setFormData({ ...formData, endTime: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Base Price *
              </label>
              <input
                type="number"
                required
                value={formData.basePrice}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    basePrice: parseInt(e.target.value),
                  })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Premium
              </label>
              <input
                type="number"
                value={formData.premiumPrice}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    premiumPrice: parseInt(e.target.value),
                  })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Recliner
              </label>
              <input
                type="number"
                value={formData.reclinerPrice}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    reclinerPrice: parseInt(e.target.value),
                  })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border rounded-lg hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 bg-rose-500 text-white rounded-lg hover:bg-rose-600 disabled:opacity-50"
            >
              {saving ? "Creating..." : "Create Show"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
