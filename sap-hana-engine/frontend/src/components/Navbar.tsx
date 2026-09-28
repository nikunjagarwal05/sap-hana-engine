import React from 'react';
import { Database, LogOut, ShieldCheck } from 'lucide-react';
import { setAuthToken } from '../api/client';

export const Navbar: React.FC = () => {
  const handleLogout = () => {
    setAuthToken(null);
    window.location.href = '/login';
  };

  return (
    <header className="bg-slate-900 border-b border-slate-800 px-6 py-4 flex items-center justify-between sticky top-0 z-50">
      <div className="flex items-center space-x-3">
        <div className="bg-blue-600 p-2 rounded-lg text-white font-bold">
          <Database className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-white tracking-wide">SAP HANA Hybrid Integration Engine</h1>
          <p className="text-xs text-blue-400 font-medium">Schema-Agnostic Ingestion & Analytics</p>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-2 bg-slate-800 px-3 py-1.5 rounded-full text-xs text-emerald-400 border border-slate-700">
          <ShieldCheck className="w-4 h-4" />
          <span>Connected: Admin JWT</span>
        </div>

        <button
          onClick={handleLogout}
          className="flex items-center space-x-1 bg-red-600/20 hover:bg-red-600/30 text-red-400 px-3 py-1.5 rounded-lg text-xs font-semibold transition border border-red-500/30"
        >
          <LogOut className="w-4 h-4" />
          <span>Logout</span>
        </button>
      </div>
    </header>
  );
};
