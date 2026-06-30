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
  const [message, setMessage] = useState(`帮我查询订单 ${initialOrder.id} 什么时候开课`);
  const [chat, setChat] = useState(initialChat);
  const [chatError, setChatError] = useState<string | null>(null);

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
  const health = healthQuery.data;
  const order = orderQuery.data;
  const orderError = orderQuery.error instanceof Error ? orderQuery.error.message : null;
  const normalizedOrderId = orderId.trim();
  const canQueryOrder = tenantId.trim().length > 0 && normalizedOrderId.length > 0;
  const isOrderLookupPending = orderQuery.isFetching && orderLookupId === normalizedOrderId;
  const canSubmitChat = tenantId.trim().length > 0 && message.trim().length > 0;

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
      message: message.trim()
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

          <Card className="debug-panel chat-panel" title="Chat Console">
            <div className="chat-workbench">
              <div className="chat-form">
                <label className="field-label" htmlFor="tenant-id">
                  租户
                </label>
                <Input id="tenant-id" value={tenantId} onChange={(event) => setTenantId(event.target.value)} />

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
                  </Descriptions>
                </section>

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
