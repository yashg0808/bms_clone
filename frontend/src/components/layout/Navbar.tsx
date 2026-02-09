"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useAuthStore } from "@/store";
import { useCityStore } from "@/store";
import { locationApi } from "@/lib/api";
import { City } from "@/types";
import { Search, MapPin, User, ChevronDown, Menu, X } from "lucide-react";

export default function Navbar() {
  const { isAuthenticated, user, logout, loadFromStorage } = useAuthStore();
  const { selectedCity, setSelectedCity, setCities, cities, loadFromStorage: loadCity } = useCityStore();
  const [showCityDropdown, setShowCityDropdown] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    loadFromStorage();
    loadCity();
    fetchCities();
  }, []);

  async function fetchCities() {
    try {
      const res = await locationApi.getCities();
      const citiesData = res.data?.data || [];
      setCities(citiesData);
      if (!selectedCity && citiesData.length > 0) {
        setSelectedCity(citiesData[0]);
      }
    } catch (error) {
      console.error("Failed to fetch cities");
    }
  }

  return (
    <nav className="sticky top-0 z-50 bg-white shadow-sm">
      <div className="mx-auto max-w-7xl px-4">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-500 text-white font-bold text-lg">
              B
            </div>
            <span className="hidden text-lg font-bold text-gray-900 sm:block">
              BookMyShow
            </span>
          </Link>

          {/* Search */}
          <div className="mx-4 hidden flex-1 max-w-xl md:block">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Search for movies, events, plays..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-10 pr-4 text-sm focus:border-primary-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Right side */}
          <div className="flex items-center gap-4">
            {/* City selector */}
            <div className="relative">
              <button
                onClick={() => setShowCityDropdown(!showCityDropdown)}
                className="flex items-center gap-1 text-sm text-gray-600 hover:text-primary-500"
              >
                <MapPin className="h-4 w-4" />
                <span className="hidden sm:inline">
                  {selectedCity?.name || "Select City"}
                </span>
                <ChevronDown className="h-3 w-3" />
              </button>

              {showCityDropdown && (
                <div className="absolute right-0 top-full mt-2 w-48 rounded-lg border bg-white py-2 shadow-lg">
                  {cities.map((city: City) => (
                    <button
                      key={city.id}
                      onClick={() => {
                        setSelectedCity(city);
                        setShowCityDropdown(false);
                      }}
                      className="block w-full px-4 py-2 text-left text-sm hover:bg-gray-50"
                    >
                      {city.name}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Auth */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100"
                >
                  <User className="h-4 w-4" />
                  <span className="hidden sm:inline">
                    {user?.fullName?.split(" ")[0]}
                  </span>
                </button>

                {showUserMenu && (
                  <div className="absolute right-0 top-full mt-2 w-48 rounded-lg border bg-white py-2 shadow-lg">
                    <Link
                      href="/profile"
                      className="block px-4 py-2 text-sm hover:bg-gray-50"
                      onClick={() => setShowUserMenu(false)}
                    >
                      My Profile
                    </Link>
                    <Link
                      href="/bookings"
                      className="block px-4 py-2 text-sm hover:bg-gray-50"
                      onClick={() => setShowUserMenu(false)}
                    >
                      My Bookings
                    </Link>
                    <hr className="my-1" />
                    <button
                      onClick={() => {
                        logout();
                        setShowUserMenu(false);
                      }}
                      className="block w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-gray-50"
                    >
                      Sign Out
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <Link
                href="/login"
                className="rounded-lg bg-primary-500 px-4 py-2 text-sm font-medium text-white hover:bg-primary-600"
              >
                Sign In
              </Link>
            )}

            {/* Mobile menu toggle */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="md:hidden"
            >
              {mobileMenuOpen ? (
                <X className="h-5 w-5" />
              ) : (
                <Menu className="h-5 w-5" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileMenuOpen && (
        <div className="border-t bg-white p-4 md:hidden">
          <div className="relative mb-4">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search..."
              className="w-full rounded-lg border bg-gray-50 py-2 pl-10 pr-4 text-sm"
            />
          </div>
          <Link href="/movies" className="block py-2 text-sm">
            Movies
          </Link>
          <Link href="/bookings" className="block py-2 text-sm">
            My Bookings
          </Link>
        </div>
      )}
    </nav>
  );
}
