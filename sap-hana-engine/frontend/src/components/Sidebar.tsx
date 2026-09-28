import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Database, TableProperties, RefreshCw, BarChart3 } from 'lucide-react';

export const Sidebar: React.FC = () => {
  const links = [
    { to: '/', label: 'Overview', icon: LayoutDashboard },
    { to: '/sources', label: 'Data Sources', icon: Database },
    { to: '/entities', label: 'Entities Catalog', icon: TableProperties },
    { to: '/jobs', label: 'Ingestion Jobs', icon: RefreshCw },
    { to: '/insights', label: 'Analytics & Insights', icon: BarChart3 },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 min-h-[calc(100vh-73px)] p-4">
      <nav className="space-y-1">
        {links.map((link) => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/'}
              className={({ isActive }) =>
                `flex items-center space-x-3 px-4 py-3 rounded-xl font-medium text-sm transition-all ${
                  isActive
                    ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20'
                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`
              }
            >
              <Icon className="w-5 h-5" />
              <span>{link.label}</span>
            </NavLink>
          );
        })}
      </nav>
    </aside>
  );
};
