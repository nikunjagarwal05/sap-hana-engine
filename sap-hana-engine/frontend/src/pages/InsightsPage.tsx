import React from 'react';
import { BarChart3, AlertTriangle, TrendingUp, Sparkles } from 'lucide-react';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';

export const InsightsPage: React.FC = () => {
  const sampleFrequencyData = [
    { name: 'USD', count: 420 },
    { name: 'EUR', count: 280 },
    { name: 'INR', count: 190 },
    { name: 'GBP', count: 120 },
    { name: 'JPY', count: 85 },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-white tracking-wide">Statistical Analytics & Insights</h2>
        <p className="text-slate-400 text-sm">Automated z-score outlier detection, categorical frequency maps, and trend insights</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Outlier Alert Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-5 h-5 text-amber-400" />
            <h3 className="text-lg font-bold text-white">Anomalies Detected (|z| &gt; 3.0)</h3>
          </div>
          <p className="text-xs text-slate-400">Values deviating significantly from standard mean distribution across landed datasets</p>

          <div className="space-y-3">
            <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 flex items-center justify-between">
              <div>
                <span className="font-semibold text-sm text-white">Bill.netAmount</span>
                <p className="text-xs text-slate-400">Extreme outlier value detected: $1,450,000.00 (Mean: $12,400)</p>
              </div>
              <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-400">
                z-score: 4.8
              </span>
            </div>

            <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 flex items-center justify-between">
              <div>
                <span className="font-semibold text-sm text-white">Customer.creditLimit</span>
                <p className="text-xs text-slate-400">Unusual high credit allocation: $5,000,000.00</p>
              </div>
              <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-400">
                z-score: 3.9
              </span>
            </div>
          </div>
        </div>

        {/* Categorical Distribution Chart */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
          <div className="flex items-center space-x-2">
            <BarChart3 className="w-5 h-5 text-blue-400" />
            <h3 className="text-lg font-bold text-white">Top Categorical Frequencies (Currency)</h3>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={sampleFrequencyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="name" stroke="#94a3b8" />
                <YAxis stroke="#94a3b8" />
                <Tooltip contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px' }} />
                <Bar dataKey="count" fill="#3b82f6" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
};
