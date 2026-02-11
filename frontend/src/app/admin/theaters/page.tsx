"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api";
import { Plus, Building2, Monitor, ChevronDown, ChevronUp } from "lucide-react";

interface Theater {
  id: string;
  name: string;
  address: string;
  cityName: string;
  totalScreens: number;
}

interface Screen {
  id: string;
  name: string;
  screenType: string;
  totalSeats: number;
}

interface City {
  id: string;
  name: string;
  state: string;
}

export default function AdminTheatersPage() {
  const [theaters, setTheaters] = useState<Theater[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedTheater, setExpandedTheater] = useState<string | null>(null);
  const [theaterScreens, setTheaterScreens] = useState<
    Record<string, Screen[]>
  >({});
  const [showTheaterModal, setShowTheaterModal] = useState(false);
  const [showScreenModal, setShowScreenModal] = useState<string | null>(null);

  const fetchTheaters = async () => {
    try {
      setLoading(true);
      const response = await adminApi.getTheaters();
      setTheaters(response.data);
    } catch (error) {
      console.error("Failed to fetch theaters:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTheaters();
  }, []);

  const toggleExpand = async (theaterId: string) => {
    if (expandedTheater === theaterId) {
      setExpandedTheater(null);
    } else {
      setExpandedTheater(theaterId);
      if (!theaterScreens[theaterId]) {
        try {
          const response = await adminApi.getScreens(theaterId);
          setTheaterScreens((prev) => ({
            ...prev,
            [theaterId]: response.data,
          }));
        } catch (error) {
          console.error("Failed to fetch screens:", error);
        }
      }
    }
  };

  const handleDeleteTheater = async (id: string) => {
    if (!confirm("Are you sure you want to deactivate this theater?")) return;
    try {
      await adminApi.deleteTheater(id);
      fetchTheaters();
    } catch (error) {
      console.error("Failed to delete theater:", error);
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Theaters</h1>
          <p className="text-gray-500">Manage theaters and screens</p>
        </div>
        <button
          onClick={() => setShowTheaterModal(true)}
          className="flex items-center gap-2 bg-rose-500 text-white px-4 py-2 rounded-lg hover:bg-rose-600"
        >
          <Plus size={20} />
          Add Theater
        </button>
      </div>

      {/* Theaters List */}
      <div className="space-y-4">
        {loading ? (
          <div className="bg-white rounded-xl p-8 flex justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500"></div>
          </div>
        ) : theaters.length === 0 ? (
          <div className="bg-white rounded-xl p-12 text-center text-gray-500">
            No theaters found
          </div>
        ) : (
          theaters.map((theater) => (
            <div
              key={theater.id}
              className="bg-white rounded-xl shadow-sm overflow-hidden"
            >
              {/* Theater Header */}
              <div
                className="flex items-center justify-between p-4 cursor-pointer hover:bg-gray-50"
                onClick={() => toggleExpand(theater.id)}
              >
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-purple-100 rounded-lg">
                    <Building2 className="text-purple-500" size={24} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-lg">{theater.name}</h3>
                    <p className="text-sm text-gray-500">
                      {theater.cityName} • {theater.address}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <span className="text-sm text-gray-500">
                    {theater.totalScreens} screens
                  </span>
                  {expandedTheater === theater.id ? (
                    <ChevronUp size={20} />
                  ) : (
                    <ChevronDown size={20} />
                  )}
                </div>
              </div>

              {/* Expanded Screens */}
              {expandedTheater === theater.id && (
                <div className="border-t px-4 py-4 bg-gray-50">
                  <div className="flex justify-between items-center mb-4">
                    <h4 className="font-medium text-gray-700">Screens</h4>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowScreenModal(theater.id);
                      }}
                      className="text-sm text-rose-500 hover:text-rose-600 flex items-center gap-1"
                    >
                      <Plus size={16} />
                      Add Screen
                    </button>
                  </div>

                  {theaterScreens[theater.id]?.length ? (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                      {theaterScreens[theater.id].map((screen) => (
                        <div
                          key={screen.id}
                          className="bg-white p-4 rounded-lg border flex items-center gap-3"
                        >
                          <Monitor className="text-gray-400" size={20} />
                          <div>
                            <p className="font-medium">{screen.name}</p>
                            <p className="text-sm text-gray-500">
                              {screen.screenType} • {screen.totalSeats} seats
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-gray-500 text-sm">No screens yet</p>
                  )}

                  <div className="mt-4 pt-4 border-t flex gap-2">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteTheater(theater.id);
                      }}
                      className="text-sm text-red-500 hover:text-red-600"
                    >
                      Deactivate Theater
                    </button>
                  </div>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Create Theater Modal */}
      {showTheaterModal && (
        <TheaterModal
          onClose={() => setShowTheaterModal(false)}
          onSave={() => {
            setShowTheaterModal(false);
            fetchTheaters();
          }}
        />
      )}

      {/* Create Screen Modal */}
      {showScreenModal && (
        <ScreenModal
          theaterId={showScreenModal}
          onClose={() => setShowScreenModal(null)}
          onSave={() => {
            setShowScreenModal(null);
            // Refresh screens for this theater
            adminApi.getScreens(showScreenModal).then((res) => {
              setTheaterScreens((prev) => ({
                ...prev,
                [showScreenModal]: res.data,
              }));
            });
            fetchTheaters();
          }}
        />
      )}
    </div>
  );
}

function TheaterModal({
  onClose,
  onSave,
}: {
  onClose: () => void;
  onSave: () => void;
}) {
  const [cities, setCities] = useState<City[]>([]);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    cityId: "",
    address: "",
    phone: "",
    totalScreens: 0,
  });

  useEffect(() => {
    adminApi.getCities().then((res) => setCities(res.data));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await adminApi.createTheater(formData);
      onSave();
    } catch (error) {
      console.error("Failed to create theater:", error);
      alert("Failed to create theater");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl w-full max-w-md">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-xl font-semibold">Add New Theater</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Theater Name *
            </label>
            <input
              type="text"
              required
              value={formData.name}
              onChange={(e) =>
                setFormData({ ...formData, name: e.target.value })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              City *
            </label>
            <select
              required
              value={formData.cityId}
              onChange={(e) =>
                setFormData({ ...formData, cityId: e.target.value })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              <option value="">Select a city</option>
              {cities.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}, {c.state}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Address *
            </label>
            <textarea
              required
              value={formData.address}
              onChange={(e) =>
                setFormData({ ...formData, address: e.target.value })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
              rows={2}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Phone
            </label>
            <input
              type="tel"
              value={formData.phone}
              onChange={(e) =>
                setFormData({ ...formData, phone: e.target.value })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
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
              {saving ? "Creating..." : "Create Theater"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function ScreenModal({
  theaterId,
  onClose,
  onSave,
}: {
  theaterId: string;
  onClose: () => void;
  onSave: () => void;
}) {
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    theaterId,
    name: "",
    totalSeats: 100,
    screenType: "STANDARD",
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await adminApi.createScreen(formData);
      onSave();
    } catch (error) {
      console.error("Failed to create screen:", error);
      alert("Failed to create screen");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl w-full max-w-md">
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-xl font-semibold">Add New Screen</h2>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Screen Name *
            </label>
            <input
              type="text"
              required
              placeholder="e.g., Screen 1, IMAX, Dolby Atmos"
              value={formData.name}
              onChange={(e) =>
                setFormData({ ...formData, name: e.target.value })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Screen Type *
            </label>
            <select
              required
              value={formData.screenType}
              onChange={(e) =>
                setFormData({ ...formData, screenType: e.target.value })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            >
              <option value="STANDARD">Standard</option>
              <option value="IMAX">IMAX</option>
              <option value="DOLBY_ATMOS">Dolby Atmos</option>
              <option value="4DX">4DX</option>
              <option value="3D">3D</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Total Seats *
            </label>
            <input
              type="number"
              required
              min="1"
              value={formData.totalSeats}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  totalSeats: parseInt(e.target.value),
                })
              }
              className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500"
            />
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
              {saving ? "Creating..." : "Create Screen"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
