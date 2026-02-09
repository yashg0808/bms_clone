"use client";

import { useEffect, useState } from "react";
import { movieApi } from "@/lib/api";
import { Movie } from "@/types";
import MovieCard from "@/components/movies/MovieCard";
import { Search, Filter } from "lucide-react";

export default function MoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedGenre, setSelectedGenre] = useState("");
  const [selectedLanguage, setSelectedLanguage] = useState("");

  const genres = ["Action", "Comedy", "Drama", "Horror", "Romance", "Thriller", "Sci-Fi", "Animation"];
  const languages = ["Hindi", "English", "Tamil", "Telugu", "Malayalam", "Kannada", "Bengali"];

  useEffect(() => {
    fetchMovies();
  }, []);

  async function fetchMovies() {
    try {
      const res = await movieApi.getMovies({ page: 0, size: 50 });
      setMovies(res.data?.data?.content || []);
    } catch (error) {
      console.error("Failed to fetch movies");
    } finally {
      setLoading(false);
    }
  }

  const filteredMovies = movies.filter((movie) => {
    const matchesSearch =
      !searchQuery ||
      movie.title.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesGenre =
      !selectedGenre || movie.genre?.includes(selectedGenre);
    const matchesLanguage =
      !selectedLanguage || movie.language === selectedLanguage;
    return matchesSearch && matchesGenre && matchesLanguage;
  });

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      <h1 className="mb-6 text-3xl font-bold text-gray-900">Movies</h1>

      {/* Filters */}
      <div className="mb-8 space-y-4">
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Search movies..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg border bg-white py-2.5 pl-10 pr-4 text-sm focus:border-primary-500 focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setSelectedGenre("")}
            className={`rounded-full px-4 py-1.5 text-xs font-medium ${
              !selectedGenre
                ? "bg-primary-500 text-white"
                : "bg-gray-100 text-gray-700 hover:bg-gray-200"
            }`}
          >
            All Genres
          </button>
          {genres.map((genre) => (
            <button
              key={genre}
              onClick={() =>
                setSelectedGenre(selectedGenre === genre ? "" : genre)
              }
              className={`rounded-full px-4 py-1.5 text-xs font-medium ${
                selectedGenre === genre
                  ? "bg-primary-500 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              {genre}
            </button>
          ))}
        </div>

        <div className="flex flex-wrap gap-2">
          {languages.map((lang) => (
            <button
              key={lang}
              onClick={() =>
                setSelectedLanguage(selectedLanguage === lang ? "" : lang)
              }
              className={`rounded-full px-4 py-1.5 text-xs font-medium ${
                selectedLanguage === lang
                  ? "bg-primary-500 text-white"
                  : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              {lang}
            </button>
          ))}
        </div>
      </div>

      {/* Movies grid */}
      {loading ? (
        <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} className="h-80 animate-pulse rounded-lg bg-gray-200" />
          ))}
        </div>
      ) : filteredMovies.length > 0 ? (
        <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {filteredMovies.map((movie) => (
            <MovieCard key={movie.id} movie={movie} />
          ))}
        </div>
      ) : (
        <div className="py-20 text-center">
          <p className="text-lg text-gray-500">No movies found</p>
        </div>
      )}
    </div>
  );
}
