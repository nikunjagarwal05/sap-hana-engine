import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Database, TableProperties, Layers, RefreshCw, Activity, ArrowUpRight } from 'lucide-react';

interface SummaryData {
  totalSources: number;
  totalEntitiesDiscovered: number;
  totalRecordsLanded: number;
  totalIngestionJobs: number;
}

export const OverviewPage: React.FC = () => {
  const [summary, setSummary] = useState<SummaryData>({
    totalSources: 0,
    totalEntitiesDiscovered: 0,
    totalRecordsLanded: 0,
    totalIngestionJobs: 0,
  });
  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [sumRes, jobsRes] = await Promise.all([
        api.get('/dashboard/summary'),
        api.get('/jobs'),
      ]);
      setSummary(sumRes.data);
      setJobs(jobsRes.data);
    } catch (err) {
      console.error('Failed fetching dashboard summary', err);
    } finally {
      setLoading(false);
    }
  };

  const kpis = [
    { title: 'Configured Data Sources', value: summary.totalSources, icon: Database, color: 'from-blue-600 to-indigo-600' },
    { title: 'Discovered Entities / Tables', value: summary.totalEntitiesDiscovered, icon: TableProperties, color: 'from-purple-600 to-pink-600' },
    { title: 'Landed JSONB Records', value: summary.totalRecordsLanded.toLocaleString(), icon: Layers, color: 'from-emerald-600 to-teal-600' },
    { title: 'Total Ingestion Runs', value: summary.totalIngestionJobs, icon: RefreshCw, color: 'from-amber-600 to-orange-600' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-white tracking-wide">System Overview & KPIs</h2>
        <p className="text-slate-400 text-sm">Real-time status of SAP HANA connectors, ingestion pipeline, and metadata catalog</p>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {kpis.map((kpi, index) => {
          const Icon = kpi.icon;
          return (
            <div key={index} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 relative overflow-hidden shadow-xl">
              <div className={`absolute top-0 right-0 w-24 h-24 bg-gradient-to-br ${kpi.color} opacity-10 rounded-full blur-xl -mr-6 -mt-6`} />
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{kpi.title}</span>
                <div className={`p-2.5 rounded-xl bg-gradient-to-br ${kpi.color} text-white shadow-md`}>
                  <Icon className="w-5 h-5" />
                </div>
              </div>
              <div className="mt-4">
                <span className="text-3xl font-extrabold text-white tracking-tight">{loading ? '...' : kpi.value}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Ingestion Feed */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <Activity className="w-5 h-5 text-blue-400" />
            <h3 className="text-lg font-bold text-white">Recent Ingestion Activity</h3>
          </div>
          <button onClick={fetchData} className="text-xs text-blue-400 hover:underline flex items-center space-x-1">
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Refresh</span>
          </button>
        </div>

        {jobs.length === 0 ? (
          <div className="text-center py-8 text-slate-500 text-sm">No ingestion jobs run yet. Configure a source and trigger discovery.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-800/60 text-slate-400 text-xs uppercase font-semibold">
                <tr>
                  <th className="px-4 py-3 rounded-l-xl">Job ID</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Entities</th>
                  <th className="px-4 py-3">Records Landed</th>
                  <th className="px-4 py-3 rounded-r-xl">Started At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {jobs.slice(0, 5).map((job) => (
                  <tr key={job.id} className="hover:bg-slate-800/30 transition">
                    <td className="px-4 py-3 font-mono text-xs text-blue-400">#{job.id}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                        job.status === 'SUCCEEDED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                        job.status === 'RUNNING' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20 animate-pulse' :
                        'bg-red-500/10 text-red-400 border border-red-500/20'
                      }`}>
                        {job.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-300">{job.totalEntities}</td>
                    <td className="px-4 py-3 text-slate-300">{job.totalRecords?.toLocaleString()}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{new Date(job.startedAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
