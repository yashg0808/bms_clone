"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { movieApi } from "@/lib/api";
import { Movie, Show } from "@/types";
import { useCityStore } from "@/store";
import { formatCurrency, formatDate, formatTime } from "@/lib/utils";
import { Star, Clock, Calendar, Globe } from "lucide-react";

export default function MovieDetailPage() {
  const params = useParams();
  const router = useRouter();
  const movieId = params.id as string;
  const { selectedCity } = useCityStore();
  const [movie, setMovie] = useState<Movie | null>(null);
  const [shows, setShows] = useState<Show[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split("T")[0],
  );

  useEffect(() => {
    fetchMovie();
  }, [movieId]);

  useEffect(() => {
    if (movie && selectedCity) {
      fetchShows();
    }
  }, [movie, selectedCity, selectedDate]);

  async function fetchMovie() {
    try {
      const res = await movieApi.getMovie(movieId);
      setMovie(res.data?.data);
    } catch (error) {
      console.error("Failed to fetch movie");
    } finally {
      setLoading(false);
    }
  }

  async function fetchShows() {
    if (!selectedCity) return;
    try {
      const res = await movieApi.getMovieShows(
        movieId,
        selectedCity.id,
        selectedDate,
      );
      setShows(res.data?.data || []);
    } catch (error) {
      console.error("Failed to fetch shows");
    }
  }

  // Generate next 7 dates
  const dates = Array.from({ length: 7 }, (_, i) => {
    const date = new Date();
    date.setDate(date.getDate() + i);
    return date.toISOString().split("T")[0];
  });

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-8">
        <div className="h-96 animate-pulse rounded-lg bg-gray-200" />
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <p className="text-lg text-gray-500">Movie not found</p>
      </div>
    );
  }

  return (
    <div>
      {/* Hero section */}
      <div className="relative bg-gray-900">
        <div className="absolute inset-0 opacity-20">
          {movie.bannerUrl && (
            <img
              src={movie.bannerUrl}
              alt=""
              className="h-full w-full object-cover"
            />
          )}
        </div>
        <div className="relative mx-auto flex max-w-7xl gap-8 px-4 py-8">
          {/* Poster */}
          <div className="hidden w-64 flex-shrink-0 md:block">
            <div className="aspect-[2/3] overflow-hidden rounded-lg">
              {movie.posterUrl ? (
                <img
                  src={movie.posterUrl}
                  alt={movie.title}
                  className="h-full w-full object-cover"
                />
              ) : (
                <div className="flex h-full items-center justify-center bg-primary-600 text-6xl font-bold text-white">
                  {movie.title.charAt(0)}
                </div>
              )}
            </div>
          </div>

          {/* Info */}
          <div className="flex flex-col justify-end text-white">
            <h1 className="mb-2 text-3xl font-bold md:text-4xl">
              {movie.title}
            </h1>

            <div className="mb-4 flex flex-wrap items-center gap-3">
              {movie.averageRating > 0 && (
                <div className="flex items-center gap-1 rounded bg-green-600 px-2 py-1 text-sm font-medium">
                  <Star className="h-4 w-4 fill-white" />
                  {movie.averageRating.toFixed(1)}/10
                </div>
              )}
              <span className="text-gray-300">{movie.totalReviews} Votes</span>
            </div>

            <div className="flex flex-wrap gap-4 text-sm text-gray-300">
              <span className="flex items-center gap-1">
                <Clock className="h-4 w-4" />
                {movie.durationMinutes} min
              </span>
              <span>{movie.genre}</span>
              <span className="flex items-center gap-1">
                <Globe className="h-4 w-4" />
                {movie.language}
              </span>
              <span>{movie.rating}</span>
              <span className="flex items-center gap-1">
                <Calendar className="h-4 w-4" />
                {formatDate(movie.releaseDate)}
              </span>
            </div>

            <p className="mt-4 max-w-2xl text-sm text-gray-400">
              {movie.description}
            </p>
          </div>
        </div>
      </div>

      {/* Shows section */}
      <div className="mx-auto max-w-7xl px-4 py-8">
        <h2 className="mb-4 text-xl font-bold">Select Date & Show</h2>

        {/* Date picker */}
        <div className="mb-6 flex gap-2 overflow-x-auto pb-2">
          {dates.map((date) => {
            const d = new Date(date);
            const isSelected = date === selectedDate;
            return (
              <button
                key={date}
                onClick={() => setSelectedDate(date)}
                className={`flex flex-shrink-0 flex-col items-center rounded-lg px-4 py-2 text-sm ${
                  isSelected
                    ? "bg-primary-500 text-white"
                    : "border bg-white text-gray-700 hover:border-primary-500"
                }`}
              >
                <span className="text-xs">
                  {d.toLocaleDateString("en-US", { weekday: "short" })}
                </span>
                <span className="text-lg font-bold">{d.getDate()}</span>
                <span className="text-xs">
                  {d.toLocaleDateString("en-US", { month: "short" })}
                </span>
              </button>
            );
          })}
        </div>

        {/* Show times */}
        {shows.length > 0 ? (
          <div className="space-y-6">
            {shows.map((show) => (
              <div
                key={show.id}
                className="rounded-lg border bg-white p-4 shadow-sm"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="font-semibold">{show.theaterName}</h3>
                    <p className="text-sm text-gray-500">
                      {show.screenName} ({show.screenType})
                      {show.availableSeats != null &&
                        ` · ${show.availableSeats} seats available`}
                    </p>
                  </div>
                  <button
                    onClick={() => router.push(`/shows/${show.id}/seats`)}
                    className="rounded-lg border-2 border-green-500 px-4 py-2 text-sm font-medium text-green-600 hover:bg-green-50"
                  >
                    {formatTime(show.startTime)}
                  </button>
                </div>
                <div className="mt-2 flex gap-4 text-xs text-gray-500">
                  <span>Regular: {formatCurrency(show.basePrice)}</span>
                  <span>Premium: {formatCurrency(show.premiumPrice)}</span>
                  {show.reclinerPrice > 0 && (
                    <span>Recliner: {formatCurrency(show.reclinerPrice)}</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="rounded-lg border bg-gray-50 py-12 text-center">
            <p className="text-gray-500">
              No shows available for the selected date.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
