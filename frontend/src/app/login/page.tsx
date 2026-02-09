"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api";
import { useAuthStore } from "@/store";
import toast from "react-hot-toast";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuthStore();
  const [isRegister, setIsRegister] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phone: "",
    password: "",
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isRegister) {
        const res = await authApi.register(form);
        const { token, user } = res.data.data;
        login(token, user);
        toast.success("Account created successfully!");
      } else {
        const res = await authApi.login({
          email: form.email,
          password: form.password,
        });
        const { token, user } = res.data.data;
        login(token, user);
        toast.success("Welcome back!");
      }
      router.push("/");
    } catch (error: any) {
      const message =
        error.response?.data?.message || "Authentication failed";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[80vh] items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md rounded-xl bg-white p-8 shadow-lg">
        <div className="mb-6 text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-primary-500 text-xl font-bold text-white">
            B
          </div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isRegister ? "Create Account" : "Welcome Back"}
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            {isRegister
              ? "Sign up to book your favorite movies"
              : "Sign in to continue booking"}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Full Name
              </label>
              <input
                type="text"
                required
                value={form.fullName}
                onChange={(e) =>
                  setForm({ ...form, fullName: e.target.value })
                }
                className="w-full rounded-lg border px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
                placeholder="John Doe"
              />
            </div>
          )}

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Email
            </label>
            <input
              type="email"
              required
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="w-full rounded-lg border px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
              placeholder="you@example.com"
            />
          </div>

          {isRegister && (
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Phone
              </label>
              <input
                type="tel"
                required
                value={form.phone}
                onChange={(e) =>
                  setForm({ ...form, phone: e.target.value })
                }
                className="w-full rounded-lg border px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
                placeholder="+91 9876543210"
              />
            </div>
          )}

          <div>
            <label className="mb-1 block text-sm font-medium text-gray-700">
              Password
            </label>
            <input
              type="password"
              required
              value={form.password}
              onChange={(e) =>
                setForm({ ...form, password: e.target.value })
              }
              className="w-full rounded-lg border px-3 py-2.5 text-sm focus:border-primary-500 focus:outline-none"
              placeholder="••••••••"
              minLength={8}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-primary-500 py-2.5 font-medium text-white hover:bg-primary-600 disabled:opacity-50"
          >
            {loading
              ? "Please wait..."
              : isRegister
                ? "Create Account"
                : "Sign In"}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-gray-500">
          {isRegister ? "Already have an account?" : "Don't have an account?"}{" "}
          <button
            onClick={() => setIsRegister(!isRegister)}
            className="font-medium text-primary-500 hover:text-primary-600"
          >
            {isRegister ? "Sign In" : "Sign Up"}
          </button>
        </div>
      </div>
    </div>
  );
}
