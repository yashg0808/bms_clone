"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store";
import { User, Mail, Phone, Calendar, Shield, LogOut } from "lucide-react";
import toast from "react-hot-toast";

export default function ProfilePage() {
  const router = useRouter();
  const { user, isAuthenticated, logout } = useAuthStore();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    phone: "",
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/login");
      return;
    }
    if (user) {
      setForm({
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        phone: user.phone || "",
      });
    }
  }, [isAuthenticated, user]);

  function handleSave() {
    // TODO: call user update API
    toast.success("Profile updated successfully");
    setEditing(false);
  }

  function handleLogout() {
    logout();
    toast.success("Logged out");
    router.push("/");
  }

  if (!user) return null;

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-8 text-2xl font-bold">My Profile</h1>

      {/* Avatar Section */}
      <div className="mb-8 flex items-center gap-5">
        <div className="flex h-20 w-20 items-center justify-center rounded-full bg-primary-100 text-primary-600">
          <User className="h-10 w-10" />
        </div>
        <div>
          <h2 className="text-xl font-bold">
            {user.firstName} {user.lastName}
          </h2>
          <p className="text-gray-500">{user.email}</p>
          <span className="mt-1 inline-block rounded-full bg-primary-50 px-2 py-0.5 text-xs font-medium text-primary-600">
            {user.role || "USER"}
          </span>
        </div>
      </div>

      {/* Profile Form */}
      <div className="rounded-lg border p-6">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold">Personal Information</h3>
          {!editing && (
            <button
              onClick={() => setEditing(true)}
              className="rounded-lg border px-4 py-1.5 text-sm font-medium text-primary-600 hover:bg-primary-50"
            >
              Edit
            </button>
          )}
        </div>

        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                First Name
              </label>
              {editing ? (
                <input
                  type="text"
                  value={form.firstName}
                  onChange={(e) =>
                    setForm({ ...form, firstName: e.target.value })
                  }
                  className="w-full rounded-lg border px-3 py-2 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                />
              ) : (
                <p className="flex items-center gap-2 text-gray-800">
                  <User className="h-4 w-4 text-gray-400" />
                  {user.firstName}
                </p>
              )}
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Last Name
              </label>
              {editing ? (
                <input
                  type="text"
                  value={form.lastName}
                  onChange={(e) =>
                    setForm({ ...form, lastName: e.target.value })
                  }
                  className="w-full rounded-lg border px-3 py-2 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                />
              ) : (
                <p className="text-gray-800">{user.lastName}</p>
              )}
            </div>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Email
            </label>
            <p className="flex items-center gap-2 text-gray-800">
              <Mail className="h-4 w-4 text-gray-400" />
              {user.email}
              <span className="rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-700">
                Verified
              </span>
            </p>
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Phone
            </label>
            {editing ? (
              <input
                type="tel"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                placeholder="+91 XXXXXXXXXX"
                className="w-full rounded-lg border px-3 py-2 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
              />
            ) : (
              <p className="flex items-center gap-2 text-gray-800">
                <Phone className="h-4 w-4 text-gray-400" />
                {user.phone || "Not set"}
              </p>
            )}
          </div>

          {editing && (
            <div className="flex gap-3 pt-2">
              <button
                onClick={handleSave}
                className="rounded-lg bg-primary-500 px-6 py-2 font-semibold text-white hover:bg-primary-600"
              >
                Save Changes
              </button>
              <button
                onClick={() => setEditing(false)}
                className="rounded-lg border px-6 py-2 font-semibold text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Account Actions */}
      <div className="mt-6 space-y-3">
        <button
          onClick={() => router.push("/bookings")}
          className="flex w-full items-center gap-3 rounded-lg border p-4 text-left hover:bg-gray-50"
        >
          <Calendar className="h-5 w-5 text-gray-400" />
          <div className="flex-1">
            <p className="font-medium">My Bookings</p>
            <p className="text-sm text-gray-500">View your booking history</p>
          </div>
        </button>

        <button
          onClick={() => toast("Coming soon!")}
          className="flex w-full items-center gap-3 rounded-lg border p-4 text-left hover:bg-gray-50"
        >
          <Shield className="h-5 w-5 text-gray-400" />
          <div className="flex-1">
            <p className="font-medium">Change Password</p>
            <p className="text-sm text-gray-500">Update your password</p>
          </div>
        </button>

        <button
          onClick={handleLogout}
          className="flex w-full items-center gap-3 rounded-lg border border-red-200 p-4 text-left text-red-600 hover:bg-red-50"
        >
          <LogOut className="h-5 w-5" />
          <div className="flex-1">
            <p className="font-medium">Sign Out</p>
            <p className="text-sm text-red-400">Log out of your account</p>
          </div>
        </button>
      </div>
    </div>
  );
}
