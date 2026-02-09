import Link from "next/link";

export default function Footer() {
  return (
    <footer className="border-t bg-gray-900 text-gray-400">
      <div className="mx-auto max-w-7xl px-4 py-12">
        <div className="grid grid-cols-2 gap-8 md:grid-cols-4">
          <div>
            <h3 className="mb-4 font-semibold text-white">Movies</h3>
            <ul className="space-y-2 text-sm">
              <li><Link href="/movies" className="hover:text-white">Now Showing</Link></li>
              <li><Link href="/movies" className="hover:text-white">Coming Soon</Link></li>
              <li><Link href="/movies" className="hover:text-white">Premieres</Link></li>
            </ul>
          </div>
          <div>
            <h3 className="mb-4 font-semibold text-white">Help</h3>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-white">About Us</a></li>
              <li><a href="#" className="hover:text-white">Contact Us</a></li>
              <li><a href="#" className="hover:text-white">FAQs</a></li>
            </ul>
          </div>
          <div>
            <h3 className="mb-4 font-semibold text-white">Legal</h3>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-white">Terms & Conditions</a></li>
              <li><a href="#" className="hover:text-white">Privacy Policy</a></li>
              <li><a href="#" className="hover:text-white">Refund Policy</a></li>
            </ul>
          </div>
          <div>
            <h3 className="mb-4 font-semibold text-white">Connect</h3>
            <p className="text-sm">
              Built as a learning project. Not affiliated with BookMyShow.
            </p>
          </div>
        </div>

        <div className="mt-8 border-t border-gray-800 pt-8 text-center text-sm">
          <p>© 2025 BookMyShow Clone. Educational project.</p>
        </div>
      </div>
    </footer>
  );
}
