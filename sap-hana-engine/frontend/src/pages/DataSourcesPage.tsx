import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { Database, Plus, CheckCircle, AlertTriangle, Play, RefreshCw } from 'lucide-react';

export const DataSourcesPage: React.FC = () => {
  const [sources, setSources] = useState<any[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [testResult, setTestResult] = useState<{ [key: number]: any }>({});
  const [loading, setLoading] = useState(false);

  // Form State
  const [name, setName] = useState('');
  const [connectorType, setConnectorType] = useState('JDBC');
  const [jdbcUrl, setJdbcUrl] = useState('jdbc:sap://hana-cloud.trial.sap:443/?encrypt=true');
  const [username, setUsername] = useState('SYSTEM');
  const [password, setPassword] = useState('');
  const [restBaseUrl, setRestBaseUrl] = useState('https://sandbox.api.sap.com/subscriptionbilling/api/bill/v2');
  const [apiKey, setApiKey] = useState('');

  useEffect(() => {
    fetchSources();
  }, []);

  const fetchSources = async () => {
    try {
      const res = await api.get('/sources');
      setSources(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    const config = connectorType === 'JDBC'
      ? { jdbcUrl, username, password }
      : { restBaseUrl, apiKey, authType: 'API_KEY' };

    try {
      await api.post('/sources', {
        name,
        connectorType,
        configJson: JSON.stringify(config),
        enabled: true,
        pollIntervalSeconds: 300,
      });
      setShowModal(false);
      setName('');
      fetchSources();
    } catch (err) {
      alert('Failed to save data source');
    } finally {
      setLoading(false);
    }
  };

  const testConnection = async (id: number) => {
    setTestResult((prev) => ({ ...prev, [id]: { testing: true } }));
    try {
      const res = await api.post(`/sources/${id}/test-connection`);
      setTestResult((prev) => ({ ...prev, [id]: res.data }));
    } catch (err: any) {
      setTestResult((prev) => ({ ...prev, [id]: { success: false, message: 'Test failed' } }));
    }
  };

  const triggerIngest = async (id: number) => {
    try {
      await api.post(`/sources/${id}/ingest`);
      alert('Ingestion job triggered successfully!');
    } catch (err) {
      alert('Failed triggering ingestion');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white tracking-wide">Data Sources Management</h2>
          <p className="text-slate-400 text-sm">Register direct SAP HANA JDBC instances or REST/OData endpoints</p>
        </div>
        <button
          onClick={() => setShowModal(true)}
          className="bg-blue-600 hover:bg-blue-500 text-white font-semibold px-4 py-2.5 rounded-xl flex items-center space-x-2 shadow-lg shadow-blue-600/30 text-sm transition"
        >
          <Plus className="w-4 h-4" />
          <span>Add Data Source</span>
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {sources.map((src) => {
          const res = testResult[src.id];
          return (
            <div key={src.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4 relative">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className={`p-2.5 rounded-xl text-white ${src.connectorType === 'JDBC' ? 'bg-indigo-600' : 'bg-pink-600'}`}>
                    <Database className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white">{src.name}</h3>
                    <span className="text-xs font-mono text-slate-400 uppercase tracking-wider">{src.connectorType} Connector</span>
                  </div>
                </div>
                <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  ACTIVE
                </span>
              </div>

              {res && (
                <div className={`p-3 rounded-xl text-xs flex items-center space-x-2 ${res.success ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'}`}>
                  {res.success ? <CheckCircle className="w-4 h-4" /> : <AlertTriangle className="w-4 h-4" />}
                  <span>{res.message}</span>
                </div>
              )}

              <div className="pt-2 border-t border-slate-800 flex items-center justify-between">
                <button
                  onClick={() => testConnection(src.id)}
                  disabled={res?.testing}
                  className="text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 px-3 py-1.5 rounded-lg transition border border-slate-700 flex items-center space-x-1"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${res?.testing ? 'animate-spin' : ''}`} />
                  <span>Test Connection</span>
                </button>
                <button
                  onClick={() => triggerIngest(src.id)}
                  className="text-xs bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 px-3 py-1.5 rounded-lg transition border border-blue-500/30 flex items-center space-x-1 font-semibold"
                >
                  <Play className="w-3.5 h-3.5" />
                  <span>Discover & Ingest</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Add Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-lg w-full shadow-2xl space-y-4">
            <h3 className="text-xl font-bold text-white">Register New Data Source</h3>

            <form onSubmit={handleCreate} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Source Name</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. SAP HANA Production Trial"
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2 text-white"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-400 mb-1">Connector Protocol</label>
                <select
                  value={connectorType}
                  onChange={(e) => setConnectorType(e.target.value)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2 text-white"
                >
                  <option value="JDBC">Direct SQL JDBC (HANA Cloud ngdbc)</option>
                  <option value="REST">REST / OData v2/v4 (SAP API Hub)</option>
                </select>
              </div>

              {connectorType === 'JDBC' ? (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 mb-1">JDBC Connection URL</label>
                    <input
                      type="text"
                      value={jdbcUrl}
                      onChange={(e) => setJdbcUrl(e.target.value)}
                      className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2 text-white font-mono text-xs"
                      required
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-slate-400 mb-1">Username</label>
                      <input
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2 text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-slate-400 mb-1">Password</label>
                      <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2 text-white"
                      />
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 mb-1">OData Service Base URL</label>
                    <input
                      type="text"
                      value={restBaseUrl}
                      onChange={(e) => setRestBaseUrl(e.target.value)}
                      className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2 text-white font-mono text-xs"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 mb-1">SAP API Key (APIKey Header)</label>
                    <input
                      type="text"
                      value={apiKey}
                      onChange={(e) => setApiKey(e.target.value)}
                      className="w-full bg-slate-800 border border-slate-700 rounded-xl px-4 py-2 text-white font-mono text-xs"
                    />
                  </div>
                </>
              )}

              <div className="flex items-center justify-end space-x-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 bg-slate-800 text-slate-300 rounded-xl hover:bg-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="px-4 py-2 bg-blue-600 text-white font-semibold rounded-xl hover:bg-blue-500 shadow-lg shadow-blue-600/30"
                >
                  Save Source
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
