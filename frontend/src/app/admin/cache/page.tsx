"use client";

import { useEffect, useState } from "react";
import { adminApi } from "@/lib/api";
import { Database, Trash2, RefreshCw, AlertTriangle } from "lucide-react";

interface CacheStatus {
  [key: string]: {
    exists: boolean;
    type: string;
  };
}

const cacheTTLs: Record<string, string> = {
  movies: "1 hour",
  "movies-list": "10 min",
  "movies-by-city": "10 min",
  "featured-movies": "15 min",
  cities: "24 hours",
  theaters: "6 hours",
  shows: "5 min",
  "shows-by-movie": "5 min",
};

export default function AdminCachePage() {
  const [cacheStatus, setCacheStatus] = useState<CacheStatus>({});
  const [loading, setLoading] = useState(true);
  const [clearing, setClearing] = useState<string | null>(null);

  const fetchCacheStatus = async () => {
    try {
      setLoading(true);
      const response = await adminApi.getCacheStatus();
      setCacheStatus(response.data);
    } catch (error) {
      console.error("Failed to fetch cache status:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCacheStatus();
  }, []);

  const handleClearCache = async (cacheName: string) => {
    try {
      setClearing(cacheName);
      await adminApi.clearCache(cacheName);
      // Show success feedback briefly
      await new Promise((r) => setTimeout(r, 500));
      fetchCacheStatus();
    } catch (error) {
      console.error("Failed to clear cache:", error);
      alert("Failed to clear cache");
    } finally {
      setClearing(null);
    }
  };

  const handleClearAllCaches = async () => {
    if (
      !confirm(
        "Are you sure you want to clear ALL caches? This may temporarily impact performance.",
      )
    ) {
      return;
    }
    try {
      setClearing("all");
      await adminApi.clearAllCaches();
      await new Promise((r) => setTimeout(r, 500));
      fetchCacheStatus();
    } catch (error) {
      console.error("Failed to clear all caches:", error);
      alert("Failed to clear caches");
    } finally {
      setClearing(null);
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Cache Management</h1>
          <p className="text-gray-500">Monitor and manage Redis caches</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={fetchCacheStatus}
            className="flex items-center gap-2 px-4 py-2 border rounded-lg hover:bg-gray-50"
          >
            <RefreshCw size={18} />
            Refresh
          </button>
          <button
            onClick={handleClearAllCaches}
            disabled={clearing === "all"}
            className="flex items-center gap-2 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:opacity-50"
          >
            <Trash2 size={18} />
            {clearing === "all" ? "Clearing..." : "Clear All"}
          </button>
        </div>
      </div>

      {/* Warning */}
      <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-6 flex items-start gap-3">
        <AlertTriangle
          className="text-amber-500 flex-shrink-0 mt-0.5"
          size={20}
        />
        <div>
          <p className="font-medium text-amber-800">
            Cache Invalidation Impact
          </p>
          <p className="text-sm text-amber-700">
            Clearing caches will temporarily increase database load as data is
            re-fetched. Caches will automatically repopulate on the next
            request.
          </p>
        </div>
      </div>

      {/* Cache Grid */}
      {loading ? (
        <div className="bg-white rounded-xl p-8 flex justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-rose-500"></div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Object.entries(cacheStatus).map(([cacheName, status]) => (
            <div
              key={cacheName}
              className="bg-white rounded-xl shadow-sm p-5 border hover:border-rose-200 transition-colors"
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-blue-100 rounded-lg">
                    <Database className="text-blue-500" size={20} />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-800">{cacheName}</h3>
                    <p className="text-xs text-gray-500">{status.type}</p>
                  </div>
                </div>
                <span
                  className={`px-2 py-1 rounded-full text-xs ${
                    status.exists
                      ? "bg-green-100 text-green-700"
                      : "bg-gray-100 text-gray-500"
                  }`}
                >
                  {status.exists ? "Active" : "Empty"}
                </span>
              </div>

              <div className="mb-4">
                <p className="text-sm text-gray-500">
                  TTL:{" "}
                  <span className="font-medium">
                    {cacheTTLs[cacheName] || "30 min (default)"}
                  </span>
                </p>
              </div>

              <button
                onClick={() => handleClearCache(cacheName)}
                disabled={clearing === cacheName}
                className="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm border border-red-200 text-red-500 rounded-lg hover:bg-red-50 disabled:opacity-50"
              >
                {clearing === cacheName ? (
                  <>
                    <RefreshCw size={16} className="animate-spin" />
                    Clearing...
                  </>
                ) : (
                  <>
                    <Trash2 size={16} />
                    Clear Cache
                  </>
                )}
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Cache TTL Reference */}
      <div className="mt-8 bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">
          Cache TTL Reference
        </h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left py-2 font-medium text-gray-600">
                  Cache Name
                </th>
                <th className="text-left py-2 font-medium text-gray-600">
                  TTL
                </th>
                <th className="text-left py-2 font-medium text-gray-600">
                  Description
                </th>
              </tr>
            </thead>
            <tbody className="divide-y">
              <tr>
                <td className="py-2 font-mono text-rose-600">movies</td>
                <td className="py-2">1 hour</td>
                <td className="py-2 text-gray-500">Individual movie details</td>
              </tr>
              <tr>
                <td className="py-2 font-mono text-rose-600">movies-list</td>
                <td className="py-2">10 min</td>
                <td className="py-2 text-gray-500">Paginated movie listings</td>
              </tr>
              <tr>
                <td className="py-2 font-mono text-rose-600">movies-by-city</td>
                <td className="py-2">10 min</td>
                <td className="py-2 text-gray-500">Movies showing in a city</td>
              </tr>
              <tr>
                <td className="py-2 font-mono text-rose-600">
                  featured-movies
                </td>
                <td className="py-2">15 min</td>
                <td className="py-2 text-gray-500">Homepage featured movies</td>
              </tr>
              <tr>
                <td className="py-2 font-mono text-rose-600">cities</td>
                <td className="py-2">24 hours</td>
                <td className="py-2 text-gray-500">City dropdown list</td>
              </tr>
              <tr>
                <td className="py-2 font-mono text-rose-600">theaters</td>
                <td className="py-2">6 hours</td>
                <td className="py-2 text-gray-500">Theaters in a city</td>
              </tr>
              <tr>
                <td className="py-2 font-mono text-rose-600">shows</td>
                <td className="py-2">5 min</td>
                <td className="py-2 text-gray-500">
                  Show details (seat selection)
                </td>
              </tr>
              <tr>
                <td className="py-2 font-mono text-rose-600">shows-by-movie</td>
                <td className="py-2">5 min</td>
                <td className="py-2 text-gray-500">Show times for a movie</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
