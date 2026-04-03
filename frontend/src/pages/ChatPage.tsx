import { useState, useRef, useEffect } from 'react';
import { chatApi, workflowApi, ChatMessage, ChatResponse } from '../api/workflow';
import type { Workflow } from '../types/workflow';

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [sessionId] = useState(() => crypto.randomUUID());
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [selectedWorkflow, setSelectedWorkflow] = useState<string>('');
  const [apiKey, setApiKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [lastResponse, setLastResponse] = useState<ChatResponse | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Load workflows on mount
    workflowApi.list().then(setWorkflows).catch(console.error);
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMessage = { role: 'user' as const, content: input };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      const response = await chatApi.send(
        sessionId,
        input,
        selectedWorkflow || undefined,
        apiKey || undefined
      );

      setLastResponse(response);

      if (response.message) {
        const assistantMessage = { role: 'assistant' as const, content: response.message };
        setMessages(prev => [...prev, assistantMessage]);
      }
    } catch (error) {
      console.error('Chat error:', error);
      const errorMessage = { role: 'assistant' as const, content: 'Error: ' + (error as Error).message };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh', gap: '16px', padding: '16px' }}>
      {/* Chat Panel */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', border: '1px solid #ddd', borderRadius: '8px', overflow: 'hidden' }}>
        <div style={{ padding: '12px 16px', borderBottom: '1px solid #ddd', background: '#f5f5f5' }}>
          <h2 style={{ margin: 0, fontSize: '16px' }}>AI Chat</h2>
        </div>

        {/* Messages */}
        <div style={{ flex: 1, overflow: 'auto', padding: '16px' }}>
          {messages.length === 0 && (
            <div style={{ color: '#999', textAlign: 'center', marginTop: '40%' }}>
              Send a message to start chatting
            </div>
          )}
          {messages.map((msg, idx) => (
            <div key={idx} style={{
              display: 'flex',
              justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
              marginBottom: '12px'
            }}>
              <div style={{
                maxWidth: '70%',
                padding: '10px 14px',
                borderRadius: '12px',
                background: msg.role === 'user' ? '#007bff' : '#e9ecef',
                color: msg.role === 'user' ? '#fff' : '#333',
              }}>
                {msg.content}
              </div>
            </div>
          ))}
          {loading && (
            <div style={{ color: '#999', textAlign: 'center' }}>Thinking...</div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input */}
        <div style={{ padding: '12px', borderTop: '1px solid #ddd', display: 'flex', gap: '8px' }}>
          <textarea
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Type a message..."
            style={{ flex: 1, padding: '10px', border: '1px solid #ddd', borderRadius: '6px', resize: 'none', minHeight: '44px' }}
            rows={1}
          />
          <button
            onClick={handleSend}
            disabled={loading || !input.trim()}
            style={{
              padding: '10px 20px',
              background: loading ? '#ccc' : '#007bff',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            Send
          </button>
        </div>
      </div>

      {/* Settings Panel */}
      <div style={{ width: '280px', border: '1px solid #ddd', borderRadius: '8px', padding: '16px', height: 'fit-content' }}>
        <h3 style={{ margin: '0 0 16px', fontSize: '14px' }}>Settings</h3>

        <div style={{ marginBottom: '16px' }}>
          <label style={{ display: 'block', fontSize: '12px', color: '#666', marginBottom: '4px' }}>API Key (optional)</label>
          <input
            type="password"
            value={apiKey}
            onChange={e => setApiKey(e.target.value)}
            placeholder="sk-..."
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ marginBottom: '16px' }}>
          <label style={{ display: 'block', fontSize: '12px', color: '#666', marginBottom: '4px' }}>Workflow (optional)</label>
          <select
            value={selectedWorkflow}
            onChange={e => setSelectedWorkflow(e.target.value)}
            style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
          >
            <option value="">No workflow (echo mode)</option>
            {workflows.map(w => (
              <option key={w.id} value={w.id}>{w.name}</option>
            ))}
          </select>
        </div>

        {/* Execution Chain */}
        {lastResponse && lastResponse.chain && lastResponse.chain.length > 0 && (
          <div>
            <h4 style={{ margin: '0 0 8px', fontSize: '12px', color: '#666' }}>Execution Chain</h4>
            <div style={{ background: '#f5f5f5', borderRadius: '4px', padding: '8px', fontSize: '11px' }}>
              {lastResponse.chain.map((node, idx) => (
                <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
                  <span style={{
                    width: '8px', height: '8px', borderRadius: '50%',
                    background: node.status === 'SUCCESS' ? '#28a745' : '#dc3545'
                  }} />
                  <span>{node.nodeId}</span>
                  <span style={{ color: '#666' }}>({node.status})</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {lastResponse && lastResponse.workflowExecutionId && (
          <div style={{ marginTop: '12px', fontSize: '11px', color: '#666' }}>
            Execution: {lastResponse.workflowExecutionId}
          </div>
        )}
      </div>
    </div>
  );
}
