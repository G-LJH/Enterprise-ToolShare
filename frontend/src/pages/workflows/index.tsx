import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { App, Button, Card, Col, Input, Row, Space, Tag, Typography } from 'antd';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Paragraph, Text } = Typography;

type WorkflowListItem = {
  id: number;
  name: string;
  scenario: string;
  description: string;
  featured: boolean;
  creatorId: number;
  creatorName: string;
  toolCount: number;
  tools: { id: number; name: string; summary?: string | null; url: string }[];
  createdAt: string;
  updatedAt: string;
};

export default function WorkflowListPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [booting, setBooting] = useState(true);
  const [loading, setLoading] = useState(false);
  const [scenario, setScenario] = useState('');
  const [featuredOnly, setFeaturedOnly] = useState(false);
  const [workflows, setWorkflows] = useState<WorkflowListItem[]>([]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (scenario.trim()) {
        params.set('scenario', scenario.trim());
      }
      if (featuredOnly) {
        params.set('featuredOnly', 'true');
      }
      const query = params.toString();
      const data = await apiRequest<WorkflowListItem[]>(`/api/workflows${query ? `?${query}` : ''}`);
      setWorkflows(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工作流失败');
    } finally {
      setLoading(false);
    }
  }, [featuredOnly, message, scenario]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        setCurrentUser(user);
        return loadData();
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadData, message, router]);

  const handleLogout = async () => {
    try {
      await apiRequest<void>('/api/auth/logout', { method: 'POST' });
    } catch {
      // Ignore logout failures and clear local session anyway.
    } finally {
      clearAuthSession();
      void router.replace('/login');
    }
  };

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载工作流目录...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="工作流"
      subtitle="把工具组合沉淀成标准化做事方法"
      section="workflows"
    >
          <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
            <Space direction="vertical" size={12}>
              <Tag color="blue" style={{ width: 'fit-content', margin: 0 }}>Workflow Library</Tag>
              <Title level={2} style={{ margin: 0 }}>把常用工具组合成可复用的方法论</Title>
            </Space>
          </Card>

          <Card bordered={false} className="dashboard-card" style={{ marginBottom: 24 }}>
            <Row gutter={[12, 12]}>
              <Col xs={24} md={12}>
                <Input.Search
                  placeholder="按场景搜索，例如：行业调研、PPT 制作、内容策划"
                  value={scenario}
                  onChange={(event) => setScenario(event.target.value)}
                  onSearch={() => void loadData()}
                />
              </Col>
              <Col xs={24} md={12}>
                <Space style={{ width: '100%', justifyContent: 'flex-end' }} wrap>
                  <Button type={featuredOnly ? 'primary' : 'default'} style={featuredOnly ? { background: '#183153' } : undefined} onClick={() => setFeaturedOnly((value) => !value)}>
                    {featuredOnly ? '只看精选中' : '切换精选筛选'}
                  </Button>
                  <Button onClick={() => void loadData()} loading={loading}>刷新</Button>
                </Space>
              </Col>
            </Row>
          </Card>

          <Row gutter={[24, 24]}>
            {workflows.map((workflow) => (
              <Col key={workflow.id} xs={24} lg={12}>
                <Card bordered={false} className="dashboard-card" style={{ height: '100%', background: '#fffdf7' }}>
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    <Space wrap>
                      {workflow.featured ? <Tag color="gold">精选</Tag> : null}
                      <Tag>{workflow.scenario}</Tag>
                      <Text type="secondary">创建人：{workflow.creatorName}</Text>
                    </Space>
                    <div>
                      <Title level={3} style={{ marginBottom: 8 }}>
                        <Link href={`/workflows/${workflow.id}`}>{workflow.name}</Link>
                      </Title>
                      <Paragraph style={{ marginBottom: 0 }}>{workflow.description}</Paragraph>
                    </div>
                    <Space wrap>
                      {workflow.tools.map((tool) => (
                        <Tag key={tool.id} color="blue">{tool.name}</Tag>
                      ))}
                    </Space>
                    <Text type="secondary">
                      共关联 {workflow.toolCount} 个工具，更新于 {new Date(workflow.updatedAt).toLocaleString('zh-CN')}
                    </Text>
                  </Space>
                </Card>
              </Col>
            ))}
            {workflows.length === 0 ? (
              <Col span={24}>
                <Card bordered={false} className="dashboard-card">
                  <Text type="secondary">当前没有匹配的工作流。</Text>
                </Card>
              </Col>
            ) : null}
          </Row>
    </EnterpriseShell>
  );
}
