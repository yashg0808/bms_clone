"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api";
import { Plus, Pencil, Trash2, Eye, EyeOff, Search, X } from "lucide-react";

interface Movie {
  id: string;
  title: string;
  language: string;
  genre: string;
  durationMinutes: number;
  releaseDate: string;
  rating: string;
  imdbRating: number;
  posterUrl: string;
  isActive: boolean;
}

interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export default function AdminMoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [editingMovie, setEditingMovie] = useState<Movie | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  const fetchMovies = async () => {
    try {
      setLoading(true);
      const response = await adminApi.getMovies({ page, size: 10 });
      const data: PagedResponse<Movie> = response.data;
      setMovies(data.content);
      setTotalPages(data.totalPages);
    } catch (error) {
      console.error("Failed to fetch movies:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMovies();
  }, [page]);

  const handleToggleActive = async (movie: Movie) => {
    try {
      await adminApi.toggleMovieActive(movie.id);
      fetchMovies();
    } catch (error) {
      console.error("Failed to toggle movie:", error);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Are you sure you want to deactivate this movie?")) return;
    try {
      await adminApi.deleteMovie(id);
      fetchMovies();
    } catch (error) {
      console.error("Failed to delete movie:", error);
    }
  };

  const openCreateModal = () => {
    setEditingMovie(null);
    setShowModal(true);
  };

  const openEditModal = (movie: Movie) => {
    setEditingMovie(movie);
    setShowModal(true);
  };

  const filteredMovies = movies.filter(
    (m) =>
      m.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.language.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.genre.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Movies</h1>
          <p className="text-gray-500">Manage movie catalog</p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center gap-2 bg-rose-500 text-white px-4 py-2 rounded-lg hover:bg-rose-600"
        >
          <Plus size={20} />
          Add Movie
        </button>
      </div>

      {/* Search */}
      <div className="bg-white rounded-xl shadow-sm p-4 mb-6">
        <div className="relative">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
            size={20}
          />
          <input
            type="text"
            placeholder="Search movies..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
          />
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
                  Movie
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Language
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Genre
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Duration
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Rating
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredMovies.map((movie) => (
                <tr key={movie.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      {movie.posterUrl && (
                        <img
                          src={movie.posterUrl}
                          alt={movie.title}
                          className="w-10 h-14 object-cover rounded"
                        />
                      )}
                      <div>
                        <p className="font-medium text-gray-900">
                          {movie.title}
                        </p>
                        <p className="text-sm text-gray-500">
                          {movie.releaseDate}
                        </p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-600">{movie.language}</td>
                  <td className="px-6 py-4 text-gray-600">{movie.genre}</td>
                  <td className="px-6 py-4 text-gray-600">
                    {movie.durationMinutes} min
                  </td>
                  <td className="px-6 py-4">
                    <span className="flex items-center gap-1">
                      <span className="text-yellow-500">★</span>
                      {movie.imdbRating || "N/A"}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`px-2 py-1 rounded-full text-xs ${
                        movie.isActive
                          ? "bg-green-100 text-green-700"
                          : "bg-red-100 text-red-700"
                      }`}
                    >
                      {movie.isActive ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => handleToggleActive(movie)}
                        className="p-2 text-gray-500 hover:text-rose-500"
                        title={movie.isActive ? "Deactivate" : "Activate"}
                      >
                        {movie.isActive ? (
                          <EyeOff size={18} />
                        ) : (
                          <Eye size={18} />
                        )}
                      </button>
                      <button
                        onClick={() => openEditModal(movie)}
                        className="p-2 text-gray-500 hover:text-blue-500"
                        title="Edit"
                      >
                        <Pencil size={18} />
                      </button>
                      <button
                        onClick={() => handleDelete(movie.id)}
                        className="p-2 text-gray-500 hover:text-red-500"
                        title="Delete"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
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

      {/* Modal */}
      {showModal && (
        <MovieModal
          movie={editingMovie}
          onClose={() => setShowModal(false)}
          onSave={() => {
            setShowModal(false);
            fetchMovies();
          }}
        />
      )}
    </div>
  );
}

function MovieModal({
  movie,
  onClose,
  onSave,
}: {
  movie: Movie | null;
  onClose: () => void;
  onSave: () => void;
}) {
  const [formData, setFormData] = useState({
    title: movie?.title || "",
    description: "",
    durationMinutes: movie?.durationMinutes || 120,
    language: movie?.language || "",
    genre: movie?.genre || "",
    releaseDate: movie?.releaseDate || "",
    rating: movie?.rating || "",
    imdbRating: movie?.imdbRating || 0,
    posterUrl: movie?.posterUrl || "",
    bannerUrl: "",
    trailerUrl: "",
    isActive: movie?.isActive ?? true,
  });
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      if (movie) {
        await adminApi.updateMovie(movie.id, formData);
      } else {
        await adminApi.createMovie(formData);
      }
      onSave();
    } catch (error) {
      console.error("Failed to save movie:", error);
      alert("Failed to save movie");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-xl font-semibold">
            {movie ? "Edit Movie" : "Add New Movie"}
          </h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Title *
              </label>
              <input
                type="text"
                required
                value={formData.title}
                onChange={(e) =>
                  setFormData({ ...formData, title: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Language *
              </label>
              <input
                type="text"
                required
                value={formData.language}
                onChange={(e) =>
                  setFormData({ ...formData, language: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Genre *
              </label>
              <input
                type="text"
                required
                value={formData.genre}
                onChange={(e) =>
                  setFormData({ ...formData, genre: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Duration (minutes) *
              </label>
              <input
                type="number"
                required
                value={formData.durationMinutes}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    durationMinutes: parseInt(e.target.value),
                  })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Release Date *
              </label>
              <input
                type="date"
                required
                value={formData.releaseDate}
                onChange={(e) =>
                  setFormData({ ...formData, releaseDate: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Rating (UA, U, A, etc.)
              </label>
              <input
                type="text"
                value={formData.rating}
                onChange={(e) =>
                  setFormData({ ...formData, rating: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                IMDB Rating
              </label>
              <input
                type="number"
                step="0.1"
                min="0"
                max="10"
                value={formData.imdbRating}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    imdbRating: parseFloat(e.target.value),
                  })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Poster URL
              </label>
              <input
                type="url"
                value={formData.posterUrl}
                onChange={(e) =>
                  setFormData({ ...formData, posterUrl: e.target.value })
                }
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div className="col-span-2 flex items-center gap-2">
              <input
                type="checkbox"
                id="isActive"
                checked={formData.isActive}
                onChange={(e) =>
                  setFormData({ ...formData, isActive: e.target.checked })
                }
                className="rounded border-gray-300"
              />
              <label htmlFor="isActive" className="text-sm text-gray-700">
                Active (visible to users)
              </label>
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
              {saving ? "Saving..." : movie ? "Update Movie" : "Create Movie"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
