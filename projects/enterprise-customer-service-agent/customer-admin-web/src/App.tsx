import { App as AntApp, Badge, Card, ConfigProvider, Layout, Space, Typography } from 'antd';
import './styles.css';

const dayTwoModules = [
  'customer-agent-app',
  'customer-domain',
  'customer-mcp-server',
  'customer-admin-web'
];

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
        <Layout className="debug-shell">
          <Layout.Header className="debug-header">
            <Typography.Title level={1}>Customer Agent Debug Console</Typography.Title>
            <Badge status="processing" text="Day 02 skeleton" />
          </Layout.Header>
          <Layout.Content className="debug-content">
            <section className="debug-intro">
              <Typography.Title level={2}>企业级智能客服与订单协同 Agent 平台</Typography.Title>
              <Typography.Paragraph>
                当前调试台只建立本地前端骨架，后续会逐步接入对话测试、订单查询、工具调用链和 RAG 来源查看。
              </Typography.Paragraph>
            </section>
            <section className="module-grid" aria-label="Day 02 modules">
              {dayTwoModules.map((moduleName) => (
                <Card key={moduleName} className="module-card">
                  <Space direction="vertical" size={4}>
                    <Typography.Text strong>{moduleName}</Typography.Text>
                    <Typography.Text type="secondary">ready for Day 03+</Typography.Text>
                  </Space>
                </Card>
              ))}
            </section>
          </Layout.Content>
        </Layout>
      </AntApp>
    </ConfigProvider>
  );
}

export default App;
