import { QueryClient, QueryClientProvider, useMutation, useQuery } from '@tanstack/react-query';
import {
  Alert,
  App as AntApp,
  Badge,
  Button,
  Card,
  ConfigProvider,
  Descriptions,
  Divider,
  Empty,
  Input,
  Layout,
  Tag,
  Typography
} from 'antd';
import { Send } from 'lucide-react';
import { useState } from 'react';
import './styles.css';

type HealthResponse = {
  status: string;
  service: string;
};

type OrderResponse = {
  id: string;
  tenantId: string;
  customerId: string;
  productName: string;
  status: string;
  paidAt: string;
};

type CustomerAgentResponse = {
  traceId: string;
  conversationId: string;
  memorySummary: string;
  route: string;
  riskLevel: string;
  answer: string;
  sources: string[];
  nextActions: string[];
  toolCalls: ToolCall[];
};

type ToolCall = {
  name: string;
  arguments: Record<string, string>;
  status: string;
  riskLevel: string;
  durationMs: number;
  resultSummary: string;
};

type ApiErrorResponse = {
  errorCode?: string;
  message?: string;
  status?: number;
  traceId?: string;
};

type KnowledgeItemResponse = {
  itemId: string;
  tenantId: string;
  indexedChunks: number;
  skipped: boolean;
};

type KnowledgeReindexResponse = {
  documents: number;
  indexedChunks: number;
  skippedItems: number;
};

type KnowledgeDeleteResponse = {
  itemId: string;
  tenantId: string;
  deleted: boolean;
};

type KnowledgeItemSummary = {
  itemId: string;
  tenantId: string;
  category: string;
  title: string;
  source: string;
  version: string;
  indexedChunks: number;
  contentPreview: string;
};

type KnowledgeItemsResponse = {
  items: KnowledgeItemSummary[];
};

type KnowledgeSearchMatch = {
  itemId: string;
  title: string;
  source: string;
  tenant: string;
  category: string;
  content: string;
  score: number;
};

type KnowledgeSearchResponse = {
  query: string;
  tenantId: string;
  topK: number;
  matches: KnowledgeSearchMatch[];
};

type ApprovalAction = 'REFUND_ORDER' | 'CANCEL_ORDER' | 'RESCHEDULE_ORDER';

type ApprovalResponse = {
  id: string;
  tenantId: string;
  orderId: string;
  action: ApprovalAction;
  riskLevel: string;
  status: string;
  reason: string;
  redactedTrace: string;
  requiresHumanDecision: boolean;
  executed: boolean;
  requestedAt: string;
};

const queryClient = new QueryClient();

const initialHealth: HealthResponse = {
  status: 'UP',
  service: 'customer-agent-app'
};

const initialOrder: OrderResponse = {
  id: 'order-1001',
  tenantId: 'tenant-demo',
  customerId: 'customer-1001',
  productName: '企业级 AI Agent 实战营',
  status: 'PAID',
  paidAt: '2026-06-01T10:00:00Z'
};

const initialChat: CustomerAgentResponse = {
  traceId: 'trace-demo',
  conversationId: 'debug-session',
  memorySummary: '最近订单 order-1001；route=ORDER_LOOKUP；用户=帮我查询订单 order-1001 什么时候开课',
  route: 'ORDER_LOOKUP',
  riskLevel: 'READ_ONLY',
  answer: '已查询到订单 order-1001，课程为「企业级 AI Agent 实战营」，当前状态为 PAID。',
  sources: ['order:order-1001'],
  nextActions: ['展示订单状态', '等待用户继续追问'],
  toolCalls: [
    {
      name: 'order_lookup',
      arguments: {
        orderId: 'order-1001',
        tenantId: 'tenant-demo'
      },
      status: 'SUCCEEDED',
      riskLevel: 'READ_ONLY',
      durationMs: 3,
      resultSummary: 'order-1001 企业级 AI Agent 实战营 PAID'
    }
  ]
};

const initialKnowledgeItem = {
  itemId: 'faq-day20-api',
  category: 'FAQ',
  title: 'Day20 知识管理 API',
  content: '知识库管理 API 新增知识后，无需重启服务即可被 RAG 检索命中。',
  source: 'day20#api',
  version: '2026-06-30'
};

const initialApproval: ApprovalResponse = {
  id: 'approval-demo',
  tenantId: initialOrder.tenantId,
  orderId: initialOrder.id,
  action: 'REFUND_ORDER',
  riskLevel: 'HIGH_RISK',
  status: 'PENDING',
  reason: '用户申请退款，等待人工审批。',
  redactedTrace: 'trace=trace-demo orderId=order-1001 action=REFUND_ORDER executed=false',
  requiresHumanDecision: true,
  executed: false,
  requestedAt: '2026-07-01T09:40:00Z'
};

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers
    }
  });

  if (!response.ok) {
    let body: ApiErrorResponse | undefined;
    try {
      body = (await response.json()) as ApiErrorResponse;
    } catch {
      body = undefined;
    }
    throw new Error(body?.message || body?.errorCode || `Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

function tenantHeaders(tenantId: string): Record<string, string> {
  return {
    'X-Tenant-ID': tenantId.trim()
  };
}

function DebugDashboard() {
  const [tenantId, setTenantId] = useState(initialOrder.tenantId);
  const [orderId, setOrderId] = useState(initialOrder.id);
  const [orderLookupId, setOrderLookupId] = useState(initialOrder.id);
  const [conversationId, setConversationId] = useState(initialChat.conversationId);
  const [message, setMessage] = useState(`帮我查询订单 ${initialOrder.id} 什么时候开课`);
  const [chat, setChat] = useState(initialChat);
  const [chatError, setChatError] = useState<string | null>(null);
  const [knowledgeItemId, setKnowledgeItemId] = useState(initialKnowledgeItem.itemId);
  const [knowledgeTitle, setKnowledgeTitle] = useState(initialKnowledgeItem.title);
  const [knowledgeContent, setKnowledgeContent] = useState(initialKnowledgeItem.content);
  const [knowledgeSearch, setKnowledgeSearch] = useState('知识库管理');
  const [knowledgeResult, setKnowledgeResult] = useState<KnowledgeItemResponse | null>(null);
  const [deleteResult, setDeleteResult] = useState<KnowledgeDeleteResponse | null>(null);
  const [reindexResult, setReindexResult] = useState<KnowledgeReindexResponse | null>(null);
  const [knowledgeItems, setKnowledgeItems] = useState<KnowledgeItemSummary[]>([]);
  const [searchResult, setSearchResult] = useState<KnowledgeSearchResponse | null>(null);
  const [knowledgeError, setKnowledgeError] = useState<string | null>(null);
  const [approvalAction, setApprovalAction] = useState<ApprovalAction>(initialApproval.action);
  const [approvalReason, setApprovalReason] = useState('用户密码是 123456，申请退款');
  const [approvalResult, setApprovalResult] = useState<ApprovalResponse | null>(initialApproval);
  const [approvalError, setApprovalError] = useState<string | null>(null);

  const healthQuery = useQuery({
    queryKey: ['health'],
    queryFn: () => requestJson<HealthResponse>('/health'),
    initialData: initialHealth
  });
  const orderQuery = useQuery({
    queryKey: ['order', orderLookupId, tenantId.trim()],
    queryFn: () =>
      requestJson<OrderResponse>(`/api/orders/${orderLookupId}`, {
        headers: tenantHeaders(tenantId)
      }),
    enabled: tenantId.trim().length > 0 && orderLookupId.trim().length > 0,
    initialData: initialOrder,
    retry: false
  });
  const chatMutation = useMutation({
    mutationFn: (payload: ChatRequestPayload) =>
      requestJson<CustomerAgentResponse>('/chat', {
        method: 'POST',
        headers: tenantHeaders(payload.tenantId),
        body: JSON.stringify(payload)
      }),
    onError: (error) => {
      setChatError(error instanceof Error ? error.message : '请求失败');
    },
    onSuccess: (response) => {
      setChat(response);
      setChatError(null);
    }
  });
  const knowledgeMutation = useMutation({
    mutationFn: (payload: KnowledgeItemPayload) =>
      requestJson<KnowledgeItemResponse>('/admin/api/v1/knowledge/items', {
        method: 'POST',
        headers: tenantHeaders(payload.tenantId),
        body: JSON.stringify(payload.item)
      }),
    onError: (error) => {
      setKnowledgeError(error instanceof Error ? error.message : '知识写入失败');
    },
    onSuccess: (response) => {
      setKnowledgeResult(response);
      setDeleteResult(null);
      setKnowledgeError(null);
      listKnowledgeMutation.mutate({ tenantId: tenantId.trim() });
    }
  });
  const listKnowledgeMutation = useMutation({
    mutationFn: (payload: { tenantId: string }) =>
      requestJson<KnowledgeItemsResponse>('/admin/api/v1/knowledge/items', {
        headers: tenantHeaders(payload.tenantId)
      }),
    onError: (error) => {
      setKnowledgeError(error instanceof Error ? error.message : '知识列表加载失败');
    },
    onSuccess: (response) => {
      setKnowledgeItems(response.items);
      setKnowledgeError(null);
    }
  });
  const deleteKnowledgeMutation = useMutation({
    mutationFn: (payload: { tenantId: string; itemId: string }) =>
      requestJson<KnowledgeDeleteResponse>(`/admin/api/v1/knowledge/items?itemId=${encodeURIComponent(payload.itemId)}`, {
        method: 'DELETE',
        headers: tenantHeaders(payload.tenantId)
      }),
    onError: (error) => {
      setKnowledgeError(error instanceof Error ? error.message : '知识删除失败');
    },
    onSuccess: (response) => {
      setDeleteResult(response);
      setKnowledgeError(null);
      setKnowledgeItems((items) => items.filter((item) => item.itemId !== response.itemId));
    }
  });
  const searchKnowledgeMutation = useMutation({
    mutationFn: (payload: { tenantId: string; query: string }) =>
      requestJson<KnowledgeSearchResponse>(
        `/admin/api/v1/knowledge/search?query=${encodeURIComponent(payload.query)}&topK=3`,
        {
          headers: tenantHeaders(payload.tenantId)
        }
      ),
    onError: (error) => {
      setKnowledgeError(error instanceof Error ? error.message : '知识搜索失败');
    },
    onSuccess: (response) => {
      setSearchResult(response);
      setKnowledgeError(null);
    }
  });
  const reindexMutation = useMutation({
    mutationFn: (payload: { tenantId: string }) =>
      requestJson<KnowledgeReindexResponse>('/admin/api/v1/knowledge/reindex', {
        method: 'POST',
        headers: tenantHeaders(payload.tenantId)
      }),
    onError: (error) => {
      setKnowledgeError(error instanceof Error ? error.message : '重建索引失败');
    },
    onSuccess: (response) => {
      setReindexResult(response);
      setKnowledgeError(null);
      listKnowledgeMutation.mutate({ tenantId: tenantId.trim() });
    }
  });
  const approvalMutation = useMutation({
    mutationFn: (payload: ApprovalCreatePayload) =>
      requestJson<ApprovalResponse>('/api/v1/approvals', {
        method: 'POST',
        headers: tenantHeaders(payload.tenantId),
        body: JSON.stringify(payload.request)
      }),
    onError: (error) => {
      setApprovalError(error instanceof Error ? error.message : '审批创建失败');
    },
    onSuccess: (response) => {
      setApprovalResult(response);
      setApprovalReason(response.reason);
      setApprovalError(null);
    }
  });
  const health = healthQuery.data;
  const order = orderQuery.data;
  const orderError = orderQuery.error instanceof Error ? orderQuery.error.message : null;
  const normalizedOrderId = orderId.trim();
  const canQueryOrder = tenantId.trim().length > 0 && normalizedOrderId.length > 0;
  const isOrderLookupPending = orderQuery.isFetching && orderLookupId === normalizedOrderId;
  const canSubmitChat = tenantId.trim().length > 0 && message.trim().length > 0;
  const canSaveKnowledge =
    tenantId.trim().length > 0 &&
    knowledgeItemId.trim().length > 0 &&
    knowledgeTitle.trim().length > 0 &&
    knowledgeContent.trim().length > 0;
  const canDeleteKnowledge = tenantId.trim().length > 0 && knowledgeItemId.trim().length > 0;
  const canSearchKnowledge = tenantId.trim().length > 0 && knowledgeSearch.trim().length > 0;
  const canCreateApproval =
    tenantId.trim().length > 0 && normalizedOrderId.length > 0 && approvalReason.trim().length > 0;

  function submitOrderLookup() {
    if (!canQueryOrder) {
      return;
    }
    setOrderLookupId(normalizedOrderId);
  }

  function submitChat() {
    if (!canSubmitChat) {
      return;
    }
    chatMutation.mutate({
      tenantId: tenantId.trim(),
      message: message.trim(),
      conversationId: conversationId.trim()
    });
  }

  function saveKnowledge() {
    if (!canSaveKnowledge) {
      return;
    }
    knowledgeMutation.mutate({
      tenantId: tenantId.trim(),
      item: {
        itemId: knowledgeItemId.trim(),
        category: initialKnowledgeItem.category,
        title: knowledgeTitle.trim(),
        content: knowledgeContent.trim(),
        source: initialKnowledgeItem.source,
        version: initialKnowledgeItem.version,
        tags: ['day20', 'debug']
      }
    });
  }

  function reindexKnowledge() {
    if (tenantId.trim().length === 0) {
      return;
    }
    reindexMutation.mutate({ tenantId: tenantId.trim() });
  }

  function listKnowledge() {
    if (tenantId.trim().length === 0) {
      return;
    }
    listKnowledgeMutation.mutate({ tenantId: tenantId.trim() });
  }

  function deleteKnowledge() {
    if (!canDeleteKnowledge) {
      return;
    }
    deleteKnowledgeMutation.mutate({
      tenantId: tenantId.trim(),
      itemId: knowledgeItemId.trim()
    });
  }

  function searchKnowledgeItems() {
    if (!canSearchKnowledge) {
      return;
    }
    searchKnowledgeMutation.mutate({
      tenantId: tenantId.trim(),
      query: knowledgeSearch.trim()
    });
  }

  function createApproval() {
    if (!canCreateApproval) {
      return;
    }
    approvalMutation.mutate({
      tenantId: tenantId.trim(),
      request: {
        orderId: normalizedOrderId,
        action: approvalAction,
        reason: approvalReason.trim()
      }
    });
  }

  return (
    <Layout className="debug-shell">
      <Layout.Header className="debug-header">
        <Typography.Title level={1}>Customer Agent Debug Console</Typography.Title>
        <Badge status={health.status === 'UP' ? 'success' : 'error'} text={`Service ${health.status}`} />
      </Layout.Header>
      <Layout.Content className="debug-content">
        <section className="debug-grid" aria-label="Agent debug console">
          <Card className="debug-panel" title="Health">
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Service">{health.service}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={health.status === 'UP' ? 'green' : 'red'}>{health.status}</Tag>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card className="debug-panel" title="Order Debug">
            <div className="order-debug-form">
              <label className="field-label" htmlFor="order-id">
                订单号
              </label>
              <Input id="order-id" value={orderId} onChange={(event) => setOrderId(event.target.value)} />
              <Button
                aria-label="查询订单"
                className="order-query-button"
                disabled={!canQueryOrder}
                loading={isOrderLookupPending}
                onClick={submitOrderLookup}
                type="primary"
              >
                查询订单
              </Button>
            </div>
            {orderError ? <Alert className="order-debug-alert" message={orderError} showIcon type="error" /> : null}
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Order">{order.id}</Descriptions.Item>
              <Descriptions.Item label="Tenant">{order.tenantId}</Descriptions.Item>
              <Descriptions.Item label="Customer">{order.customerId}</Descriptions.Item>
              <Descriptions.Item label="Product">{order.productName}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color="blue">{order.status}</Tag>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card className="debug-panel" title="Knowledge Debug">
            <div className="knowledge-debug-form">
              <label className="field-label" htmlFor="knowledge-item-id">
                知识 ID
              </label>
              <Input
                id="knowledge-item-id"
                value={knowledgeItemId}
                onChange={(event) => setKnowledgeItemId(event.target.value)}
              />

              <label className="field-label" htmlFor="knowledge-title">
                标题
              </label>
              <Input id="knowledge-title" value={knowledgeTitle} onChange={(event) => setKnowledgeTitle(event.target.value)} />

              <label className="field-label" htmlFor="knowledge-content">
                内容
              </label>
              <Input.TextArea
                id="knowledge-content"
                autoSize={{ minRows: 3, maxRows: 6 }}
                value={knowledgeContent}
                onChange={(event) => setKnowledgeContent(event.target.value)}
              />
              <div className="knowledge-actions">
                <Button
                  aria-label="保存知识"
                  disabled={!canSaveKnowledge}
                  loading={knowledgeMutation.isPending}
                  onClick={saveKnowledge}
                  type="primary"
                >
                  保存知识
                </Button>
                <Button aria-label="重建索引" loading={reindexMutation.isPending} onClick={reindexKnowledge}>
                  重建索引
                </Button>
                <Button aria-label="刷新知识列表" loading={listKnowledgeMutation.isPending} onClick={listKnowledge}>
                  刷新列表
                </Button>
                <Button
                  aria-label="删除知识"
                  danger
                  disabled={!canDeleteKnowledge}
                  loading={deleteKnowledgeMutation.isPending}
                  onClick={deleteKnowledge}
                >
                  删除知识
                </Button>
              </div>
            </div>
            <div className="knowledge-search-form">
              <label className="field-label" htmlFor="knowledge-search">
                知识搜索
              </label>
              <Input
                id="knowledge-search"
                value={knowledgeSearch}
                onChange={(event) => setKnowledgeSearch(event.target.value)}
              />
              <Button
                aria-label="搜索知识"
                disabled={!canSearchKnowledge}
                loading={searchKnowledgeMutation.isPending}
                onClick={searchKnowledgeItems}
              >
                搜索知识
              </Button>
            </div>
            {knowledgeError ? <Alert className="knowledge-alert" message={knowledgeError} showIcon type="error" /> : null}
            <div className="knowledge-debug-result">
              {knowledgeResult ? (
                <Typography.Text>
                  {knowledgeResult.itemId} indexedChunks={knowledgeResult.indexedChunks} skipped=
                  {String(knowledgeResult.skipped)}
                </Typography.Text>
              ) : null}
              {deleteResult ? (
                <Typography.Text>
                  {deleteResult.itemId} deleted={String(deleteResult.deleted)}
                </Typography.Text>
              ) : null}
              {reindexResult ? (
                <Typography.Text>
                  documents={reindexResult.documents} indexedChunks={reindexResult.indexedChunks} skippedItems=
                  {reindexResult.skippedItems}
                </Typography.Text>
              ) : null}
            </div>
            <section className="knowledge-list" aria-label="Knowledge Items">
              <Typography.Title level={3}>Knowledge Items</Typography.Title>
              {knowledgeItems.length > 0 ? (
                <ul>
                  {knowledgeItems.map((item) => (
                    <li key={`${item.tenantId}-${item.itemId}`}>
                      <div className="knowledge-item-header">
                        <Typography.Text strong>{item.title}</Typography.Text>
                        <Tag color="blue">{item.category}</Tag>
                      </div>
                      <Typography.Text code>{item.itemId}</Typography.Text>
                      <Typography.Paragraph>{item.contentPreview}</Typography.Paragraph>
                    </li>
                  ))}
                </ul>
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </section>
            <section className="knowledge-list" aria-label="Knowledge Search Results">
              <Typography.Title level={3}>Search Results</Typography.Title>
              {searchResult && searchResult.matches.length > 0 ? (
                <ul>
                  {searchResult.matches.map((match) => (
                    <li key={`${match.tenant}-${match.itemId}-${match.source}`}>
                      <div className="knowledge-item-header">
                        <Typography.Text strong>{match.title}</Typography.Text>
                        <Typography.Text code>score={match.score}</Typography.Text>
                      </div>
                      <Typography.Text code>{match.itemId}</Typography.Text>
                      <Typography.Paragraph>{match.content}</Typography.Paragraph>
                    </li>
                  ))}
                </ul>
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </section>
          </Card>

          <Card className="debug-panel" title="Approval Debug">
            <section className="approval-debug" aria-label="Approval Debug">
              <div className="approval-debug-form">
                <label className="field-label" htmlFor="approval-action">
                  审批动作
                </label>
                <select
                  id="approval-action"
                  className="native-select"
                  value={approvalAction}
                  onChange={(event) => setApprovalAction(event.target.value as ApprovalAction)}
                >
                  <option value="REFUND_ORDER">REFUND_ORDER</option>
                  <option value="CANCEL_ORDER">CANCEL_ORDER</option>
                  <option value="RESCHEDULE_ORDER">RESCHEDULE_ORDER</option>
                </select>

                <label className="field-label" htmlFor="approval-reason">
                  审批原因
                </label>
                <Input.TextArea
                  id="approval-reason"
                  autoSize={{ minRows: 3, maxRows: 6 }}
                  value={approvalReason}
                  onChange={(event) => setApprovalReason(event.target.value)}
                />

                <Button
                  aria-label="创建审批"
                  disabled={!canCreateApproval}
                  loading={approvalMutation.isPending}
                  onClick={createApproval}
                  type="primary"
                >
                  创建审批
                </Button>
              </div>
              {approvalError ? <Alert className="approval-alert" message={approvalError} showIcon type="error" /> : null}
              {approvalResult ? (
                <div className="approval-result">
                  <div className="approval-result-header">
                    <Typography.Text strong>{approvalResult.id}</Typography.Text>
                    <span className="tool-call-tags">
                      <Tag color="red">{approvalResult.riskLevel}</Tag>
                      <Tag color="orange">{approvalResult.status}</Tag>
                    </span>
                  </div>
                  <Descriptions column={1} size="small">
                    <Descriptions.Item label="Order">
                      <Typography.Text code>{approvalResult.orderId}</Typography.Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="Action">{approvalResult.action}</Descriptions.Item>
                    <Descriptions.Item label="Human Decision">
                      {String(approvalResult.requiresHumanDecision)}
                    </Descriptions.Item>
                    <Descriptions.Item label="Execution">
                      <Typography.Text code>executed={String(approvalResult.executed)}</Typography.Text>
                    </Descriptions.Item>
                  </Descriptions>
                  <Typography.Paragraph className="reply-text">{approvalResult.redactedTrace}</Typography.Paragraph>
                </div>
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </section>
          </Card>

          <Card className="debug-panel chat-panel" title="Chat Console">
            <div className="chat-workbench">
              <div className="chat-form">
                <label className="field-label" htmlFor="tenant-id">
                  租户
                </label>
                <Input id="tenant-id" value={tenantId} onChange={(event) => setTenantId(event.target.value)} />

                <label className="field-label" htmlFor="conversation-id">
                  会话
                </label>
                <Input
                  id="conversation-id"
                  value={conversationId}
                  onChange={(event) => setConversationId(event.target.value)}
                />

                <label className="field-label" htmlFor="chat-message">
                  用户消息
                </label>
                <Input.TextArea
                  id="chat-message"
                  autoSize={{ minRows: 4, maxRows: 8 }}
                  value={message}
                  onChange={(event) => setMessage(event.target.value)}
                />

                <Button
                  className="send-button"
                  icon={<Send size={16} />}
                  disabled={!canSubmitChat}
                  loading={chatMutation.isPending}
                  onClick={submitChat}
                  type="primary"
                >
                  发送
                </Button>
              </div>

              <div className="chat-result">
                {chatError ? <Alert message={chatError} showIcon type="error" /> : null}

                <section className="request-inspector" aria-label="Request Inspector">
                  <Typography.Title level={3}>Request Inspector</Typography.Title>
                  <Descriptions column={1} size="small">
                    <Descriptions.Item label="Route">
                      <Tag color="geekblue">{chat.route}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Risk Level">
                      <Tag color={chat.riskLevel === 'HIGH_RISK' ? 'red' : 'green'}>{chat.riskLevel}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Trace ID">
                      <Typography.Text code>{chat.traceId}</Typography.Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="Conversation ID">
                      <Typography.Text code>{chat.conversationId}</Typography.Text>
                    </Descriptions.Item>
                  </Descriptions>
                </section>

                <Divider className="compact-divider" />
                <Typography.Title level={3}>Memory</Typography.Title>
                <Typography.Paragraph className="reply-text">{chat.memorySummary || '无会话摘要'}</Typography.Paragraph>

                <Divider className="compact-divider" />
                <Typography.Title level={3}>Answer</Typography.Title>
                <Typography.Paragraph className="reply-text">{chat.answer}</Typography.Paragraph>

                <Divider className="compact-divider" />
                <Typography.Title level={3}>Tool Calls</Typography.Title>
                {chat.toolCalls.length > 0 ? (
                  <ul className="tool-calls">
                    {chat.toolCalls.map((toolCall) => (
                      <li key={`${toolCall.name}-${toolCall.durationMs}-${toolCall.resultSummary}`} className="tool-call-item">
                        <div className="tool-call-header">
                          <Typography.Text strong>{toolCall.name}</Typography.Text>
                          <span className="tool-call-tags">
                            <Tag color={toolCall.status === 'SUCCEEDED' ? 'green' : 'red'}>{toolCall.status}</Tag>
                            <Tag color={toolCall.riskLevel === 'READ_ONLY' ? 'blue' : 'orange'}>{toolCall.riskLevel}</Tag>
                            <Typography.Text code>{toolCall.durationMs}ms</Typography.Text>
                          </span>
                        </div>
                        <div className="tool-call-arguments">
                          {Object.entries(toolCall.arguments).map(([key, value]) => (
                            <Typography.Text code key={key}>
                              {key}={value}
                            </Typography.Text>
                          ))}
                        </div>
                        <Typography.Paragraph className="tool-call-summary">{toolCall.resultSummary}</Typography.Paragraph>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
                )}

                <Divider className="compact-divider" />
                <Typography.Title level={3}>Sources</Typography.Title>
                {chat.sources.length > 0 ? (
                  <ul className="next-actions">
                    {chat.sources.map((source) => (
                      <li key={source}>{source}</li>
                    ))}
                  </ul>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
                )}

                <Divider className="compact-divider" />
                <Typography.Title level={3}>Next Actions</Typography.Title>
                <ul className="next-actions">
                  {chat.nextActions.map((action) => (
                    <li key={action}>{action}</li>
                  ))}
                </ul>
              </div>
            </div>
          </Card>
        </section>
      </Layout.Content>
    </Layout>
  );
}

type ChatRequestPayload = {
  message: string;
  tenantId: string;
  conversationId: string;
};

type KnowledgeItemPayload = {
  tenantId: string;
  item: {
    itemId: string;
    category: string;
    title: string;
    content: string;
    source: string;
    version: string;
    tags: string[];
  };
};

type ApprovalCreatePayload = {
  tenantId: string;
  request: {
    orderId: string;
    action: ApprovalAction;
    reason: string;
  };
};

function App() {
  return (
    <ConfigProvider
      theme={{
        token: {
          borderRadius: 6,
          colorPrimary: '#2563eb',
          fontFamily:
            '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif'
        }
      }}
    >
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <DebugDashboard />
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  );
}

export default App;
