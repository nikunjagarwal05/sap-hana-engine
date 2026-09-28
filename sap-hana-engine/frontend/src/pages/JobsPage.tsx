import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { RefreshCw, CheckCircle2, XCircle, Clock } from 'lucide-react';

export const JobsPage: React.FC = () => {
  const [jobs, setJobs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchJobs();
  }, []);

  const fetchJobs = async () => {
    setLoading(true);
    try {
      const res = await api.get('/jobs');
      setJobs(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-wide">Ingestion Job & Run Audit History</h2>
          <p className="text-slate-400 text-sm">Monitor scheduled and manual SAP ingestion loop execution telemetry</p>
        </div>
        <button
          onClick={fetchJobs}
          className="bg-slate-800 hover:bg-slate-700 text-slate-200 px-4 py-2 rounded-xl flex items-center space-x-2 text-sm border border-slate-700"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Audit Logs</span>
        </button>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-800/80 text-slate-400 text-xs font-semibold uppercase">
              <tr>
                <th className="px-4 py-3 rounded-l-xl">Job ID</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Total Entities</th>
                <th className="px-4 py-3">Records Landed</th>
                <th className="px-4 py-3">Started At</th>
                <th className="px-4 py-3 rounded-r-xl">Ended At</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {jobs.map((job) => (
                <tr key={job.id} className="hover:bg-slate-800/30 transition">
                  <td className="px-4 py-3 font-mono text-xs text-blue-400">#{job.id}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center space-x-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                      job.status === 'SUCCEEDED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                      job.status === 'RUNNING' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20 animate-pulse' :
                      'bg-red-500/10 text-red-400 border border-red-500/20'
                    }`}>
                      {job.status === 'SUCCEEDED' && <CheckCircle2 className="w-3.5 h-3.5" />}
                      {job.status === 'FAILED' && <XCircle className="w-3.5 h-3.5" />}
                      {job.status === 'RUNNING' && <Clock className="w-3.5 h-3.5 animate-spin" />}
                      <span>{job.status}</span>
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-300 font-medium">{job.totalEntities}</td>
                  <td className="px-4 py-3 text-slate-300 font-medium">{job.totalRecords?.toLocaleString()}</td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{new Date(job.startedAt).toLocaleString()}</td>
                  <td className="px-4 py-3 text-slate-400 text-xs">{job.endedAt ? new Date(job.endedAt).toLocaleString() : 'In progress'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
