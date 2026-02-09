"use client";

import Link from "next/link";
import { Movie } from "@/types";
import { cn } from "@/lib/utils";
import { Star } from "lucide-react";

interface MovieCardProps {
  movie: Movie;
  variant?: "light" | "dark";
}

export default function MovieCard({ movie, variant = "light" }: MovieCardProps) {
  return (
    <Link href={`/movies/${movie.id}`} className="group block">
      <div className="relative overflow-hidden rounded-lg">
        <div className="aspect-[2/3] bg-gray-200">
          {movie.posterUrl ? (
            <img
              src={movie.posterUrl}
              alt={movie.title}
              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
          ) : (
            <div className="flex h-full items-center justify-center bg-gradient-to-br from-primary-400 to-primary-600 text-white">
              <span className="text-4xl font-bold">
                {movie.title.charAt(0)}
              </span>
            </div>
          )}
        </div>

        {/* Rating badge */}
        {movie.averageRating > 0 && (
          <div className="absolute bottom-2 left-2 flex items-center gap-1 rounded bg-black/70 px-2 py-1 text-xs text-white">
            <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
            <span>{movie.averageRating.toFixed(1)}/10</span>
            <span className="text-gray-400">
              ({movie.totalReviews} votes)
            </span>
          </div>
        )}
      </div>

      <div className="mt-2">
        <h3
          className={cn(
            "text-sm font-semibold line-clamp-1",
            variant === "dark" ? "text-white" : "text-gray-900"
          )}
        >
          {movie.title}
        </h3>
        <p
          className={cn(
            "text-xs",
            variant === "dark" ? "text-gray-400" : "text-gray-500"
          )}
        >
          {movie.genre} • {movie.language}
        </p>
      </div>
    </Link>
  );
}
