import { Routes, Route } from 'react-router-dom';
import { Layout } from './components/Layout';
import { WorkflowsPage } from './pages/WorkflowsPage';
import { NodeTestPage } from './pages/NodeTestPage';
import { WorkflowEditorPage } from './pages/WorkflowEditorPage';
import { ExecutionHistoryPage } from './pages/ExecutionHistoryPage';
import { NodeConfigPage } from './pages/NodeConfigPage';
import ChatPage from './pages/ChatPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<WorkflowsPage />} />
        <Route path="editor" element={<WorkflowEditorPage />} />
        <Route path="history" element={<ExecutionHistoryPage />} />
        <Route path="node-config" element={<NodeConfigPage />} />
        <Route path="test/:workflowId/:nodeId" element={<NodeTestPage />} />
        <Route path="chat" element={<ChatPage />} />
      </Route>
    </Routes>
  );
}