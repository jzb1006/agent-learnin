import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import {
  App as AntApp,
  Badge,
  Card,
  ConfigProvider,
  Descriptions,
  Layout,
  List,
  Space,
  Tag,
  Typography
} from 'antd';
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

type ChatResponse = {
  traceId: string;
  route: string;
  riskLevel: string;
  reply: string;
  order: OrderResponse;
  nextActions: string[];
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

const initialChat: ChatResponse = {
  traceId: 'trace-demo',
  route: 'ORDER_LOOKUP',
  riskLevel: 'READ_ONLY',
  reply: '已查询到订单 order-1001，课程为「企业级 AI Agent 实战营」，当前状态为 PAID。',
  order: initialOrder,
  nextActions: ['展示订单状态', '等待用户继续追问']
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
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

function DebugDashboard() {
  const healthQuery = useQuery({
    queryKey: ['health'],
    queryFn: () => requestJson<HealthResponse>('/health'),
    initialData: initialHealth
  });
  const orderQuery = useQuery({
    queryKey: ['order', initialOrder.id],
    queryFn: () => requestJson<OrderResponse>(`/api/orders/${initialOrder.id}`),
    initialData: initialOrder
  });
  const chatQuery = useQuery({
    queryKey: ['chat', initialOrder.id],
    queryFn: () =>
      requestJson<ChatResponse>('/chat', {
        method: 'POST',
        body: JSON.stringify({
          tenantId: initialOrder.tenantId,
          message: `帮我查询订单 ${initialOrder.id} 什么时候开课`
        })
      }),
    initialData: initialChat
  });
  const health = healthQuery.data;
  const order = orderQuery.data;
  const chat = chatQuery.data;

  return (
    <Layout className="debug-shell">
      <Layout.Header className="debug-header">
        <Typography.Title level={1}>Customer Agent Debug Console</Typography.Title>
        <Badge status={health.status === 'UP' ? 'success' : 'error'} text={`Service ${health.status}`} />
      </Layout.Header>
      <Layout.Content className="debug-content">
        <section className="debug-grid" aria-label="Day 04 API snapshots">
          <Card className="debug-panel" title="Health">
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Service">{health.service}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={health.status === 'UP' ? 'green' : 'red'}>{health.status}</Tag>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card className="debug-panel" title="Order Debug">
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
            <Space orientation="vertical" size={12}>
              <Space wrap>
                <Tag color="geekblue">{chat.route}</Tag>
                <Tag color="green">{chat.riskLevel}</Tag>
                <Tag>{chat.traceId}</Tag>
              </Space>
              <Typography.Paragraph className="reply-text">{chat.reply}</Typography.Paragraph>
              <List
                size="small"
                dataSource={chat.nextActions}
                renderItem={(action) => <List.Item>{action}</List.Item>}
              />
            </Space>
          </Card>
        </section>
      </Layout.Content>
    </Layout>
  );
}

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
