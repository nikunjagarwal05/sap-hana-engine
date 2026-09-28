import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { TableProperties, Search, BarChart, Database, Eye } from 'lucide-react';

export const EntitiesPage: React.FC = () => {
  const [sources, setSources] = useState<any[]>([]);
  const [selectedSource, setSelectedSource] = useState<number | null>(null);
  const [entities, setEntities] = useState<any[]>([]);
  const [selectedEntity, setSelectedEntity] = useState<any | null>(null);
  const [profile, setProfile] = useState<any | null>(null);
  const [records, setRecords] = useState<any[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    fetchSources();
  }, []);

  const fetchSources = async () => {
    try {
      const res = await api.get('/sources');
      setSources(res.data);
      if (res.data.length > 0) {
        setSelectedSource(res.data[0].id);
        fetchEntities(res.data[0].id);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const fetchEntities = async (sourceId: number) => {
    setSelectedSource(sourceId);
    try {
      const res = await api.get(`/sources/${sourceId}/entities`);
      setEntities(res.data);
      if (res.data.length > 0) {
        inspectEntity(res.data[0]);
      } else {
        setSelectedEntity(null);
        setProfile(null);
        setRecords([]);
      }
    } catch (err) {
      console.error(err);
    }
  };

  const inspectEntity = async (entity: any) => {
    setSelectedEntity(entity);
    try {
      const [profRes, recRes] = await Promise.all([
        api.get(`/entities/${entity.id}/profile`),
        api.get(`/entities/${entity.id}/records?page=0&size=20`),
      ]);
      setProfile(profRes.data);
      setRecords(recRes.data.content);
    } catch (err) {
      setProfile(null);
      setRecords([]);
    }
  };

  const filteredEntities = entities.filter((e) =>
    e.entityName.toLowerCase().includes(search.toLowerCase()) ||
    e.schemaName.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-wide">Discovered Entities Catalog</h2>
          <p className="text-slate-400 text-sm">Schema-agnostic table metadata, column profiles, and raw JSONB record viewer</p>
        </div>

        {/* Source Selector */}
        <div className="flex items-center space-x-2 bg-slate-900 border border-slate-800 p-1.5 rounded-xl">
          <Database className="w-4 h-4 text-blue-400 ml-2" />
          <select
            value={selectedSource || ''}
            onChange={(e) => fetchEntities(Number(e.target.value))}
            className="bg-transparent text-white text-sm font-semibold focus:outline-none pr-4"
          >
            {sources.map((s) => (
              <option key={s.id} value={s.id} className="bg-slate-900">
                {s.name} ({s.connectorType})
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Entity List */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 space-y-4">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-500" />
            <input
              type="text"
              placeholder="Search table or schema..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-slate-800 border border-slate-700 rounded-xl pl-9 pr-4 py-2 text-xs text-white focus:outline-none"
            />
          </div>

          <div className="space-y-2 max-h-[600px] overflow-y-auto pr-1">
            {filteredEntities.map((ent) => (
              <div
                key={ent.id}
                onClick={() => inspectEntity(ent)}
                className={`p-3 rounded-xl cursor-pointer transition flex items-center justify-between border ${
                  selectedEntity?.id === ent.id
                    ? 'bg-blue-600/20 border-blue-500/50 text-white'
                    : 'bg-slate-800/40 border-slate-800 text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`}
              >
                <div>
                  <h4 className="font-semibold text-sm text-white">{ent.entityName}</h4>
                  <span className="text-xs text-slate-500 font-mono">{ent.schemaName} • {ent.columns?.length || 0} cols</span>
                </div>
                <Eye className="w-4 h-4 text-slate-500" />
              </div>
            ))}
          </div>
        </div>

        {/* Right Column: Profile & Data Inspector */}
        <div className="lg:col-span-2 space-y-6">
          {selectedEntity ? (
            <>
              {/* Table Info Header */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex items-center justify-between shadow-xl">
                <div>
                  <div className="flex items-center space-x-2">
                    <TableProperties className="w-5 h-5 text-blue-400" />
                    <h3 className="text-xl font-bold text-white">{selectedEntity.entityName}</h3>
                  </div>
                  <span className="text-xs text-slate-400 font-mono">Schema: {selectedEntity.schemaName}</span>
                </div>
                <div className="text-right">
                  <span className="text-2xl font-bold text-emerald-400">{profile?.rowCount || 0}</span>
                  <p className="text-xs text-slate-400">Total Landed Records</p>
                </div>
              </div>

              {/* Column Stats Grid */}
              {profile?.columnProfiles && profile.columnProfiles.length > 0 && (
                <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
                  <div className="flex items-center space-x-2">
                    <BarChart className="w-5 h-5 text-purple-400" />
                    <h4 className="text-lg font-bold text-white">Automated Column Profiles & Stats</h4>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {profile.columnProfiles.map((col: any) => (
                      <div key={col.id} className="bg-slate-800/50 border border-slate-700/50 rounded-xl p-4 space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-sm text-white">{col.columnName}</span>
                          <span className="px-2 py-0.5 rounded text-[10px] font-mono uppercase bg-blue-500/20 text-blue-400">
                            {col.inferredSemanticType}
                          </span>
                        </div>
                        <div className="grid grid-cols-2 gap-2 text-xs text-slate-400">
                          <div>Nulls: <span className="text-white font-medium">{col.nullPercentage.toFixed(1)}%</span></div>
                          <div>Distinct: <span className="text-white font-medium">{col.distinctCount}</span></div>
                          {col.meanVal != null && <div>Mean: <span className="text-white font-medium">{col.meanVal.toFixed(2)}</span></div>}
                          {col.outlierCount > 0 && <div className="text-amber-400 font-semibold">Outliers (|z|&gt;3): {col.outlierCount}</div>}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Raw JSONB Record Explorer */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
                <h4 className="text-lg font-bold text-white">Landed JSONB Record Browser</h4>
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-800 text-slate-400 font-semibold uppercase">
                      <tr>
                        <th className="px-4 py-3 rounded-l-xl">ID</th>
                        <th className="px-4 py-3">JSON Fields (PostgreSQL JSONB)</th>
                        <th className="px-4 py-3 rounded-r-xl">Fetched At</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {records.map((rec) => (
                        <tr key={rec.id} className="hover:bg-slate-800/30 font-mono">
                          <td className="px-4 py-3 text-blue-400">#{rec.id}</td>
                          <td className="px-4 py-3 text-slate-300 max-w-xl truncate">{rec.fieldsJson}</td>
                          <td className="px-4 py-3 text-slate-400">{new Date(rec.fetchedAt).toLocaleString()}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          ) : (
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center text-slate-500">
              Select an entity to inspect column statistics and landed records.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
