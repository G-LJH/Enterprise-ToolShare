import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { App, Button, Card, List, Space, Tabs, Tag, Typography } from 'antd';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Paragraph, Text } = Typography;

type ToolListItem = {
  id: number;
  name: string;
  summary?: string | null;
  description: string;
  url?: string | null;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'OFFLINE';
  recommenderId: number;
  recommenderName: string;
  starCount: number;
  favoriteCount: number;
  commentCount: number;
  tags: { id: number; name: string }[];
  createdAt: string;
  updatedAt: string;
};

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

function toolStatusColor(status: ToolListItem['status']) {
  if (status === 'APPROVED') {
    return 'green';
  }
  if (status === 'PENDING_REVIEW') {
    return 'gold';
  }
  if (status === 'REJECTED') {
    return 'red';
  }
  if (status === 'OFFLINE') {
    return 'volcano';
  }
  return 'blue';
}

export default function FavoritesPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [tools, setTools] = useState<ToolListItem[]>([]);
  const [workflows, setWorkflows] = useState<WorkflowListItem[]>([]);
  const [booting, setBooting] = useState(true);
  const [loading, setLoading] = useState(false);

  const loadPage = useCallback(async () => {
    setLoading(true);
    try {
      const [user, favoriteTools, favoriteWorkflows] = await Promise.all([
        apiRequest<CurrentUser>('/api/auth/me'),
        apiRequest<ToolListItem[]>('/api/tools/favorites'),
        apiRequest<WorkflowListItem[]>('/api/workflows/favorites')
      ]);
      setCurrentUser(user);
      setTools(favoriteTools);
      setWorkflows(favoriteWorkflows);
    } catch (error) {
      if (error instanceof Error && error.message.includes('登录')) {
        clearAuthSession();
        void router.replace('/login');
        return;
      }
      message.error(error instanceof Error ? error.message : '加载收藏列表失败');
      void router.replace('/tools');
    } finally {
      setLoading(false);
      setBooting(false);
    }
  }, [message, router]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }
    void loadPage();
  }, [loadPage, router]);

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
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载我的收藏...</Text></div>;
  }

  const toolTab = (
    <Card bordered={false} className="dashboard-card" title={`共收藏 ${tools.length} 个工具`}>
      <List
        loading={loading}
        dataSource={tools}
        locale={{ emptyText: '你还没有收藏任何工具。' }}
        renderItem={(tool) => (
          <List.Item key={tool.id}>
            <Card size="small" style={{ width: '100%', background: '#fffdf7' }}>
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
                  <Space wrap>
                    <Link href={`/tools/${tool.id}`}>{tool.name}</Link>
                    <Tag color={toolStatusColor(tool.status)}>{tool.status}</Tag>
                  </Space>
                  <Text type="secondary">{new Date(tool.updatedAt).toLocaleString('zh-CN')}</Text>
                </Space>
                <Text type="secondary">推荐人：{tool.recommenderName}</Text>
                <Paragraph style={{ marginBottom: 0 }}>
                  {tool.summary || tool.description.slice(0, 120)}
                </Paragraph>
                <Space wrap>
                  {tool.tags.map((tag) => (
                    <Tag key={tag.id}>{tag.name}</Tag>
                  ))}
                </Space>
                <Text type="secondary">
                  点赞 {tool.starCount} / 收藏 {tool.favoriteCount} / 评论 {tool.commentCount}
                </Text>
              </Space>
            </Card>
          </List.Item>
        )}
      />
    </Card>
  );

  const workflowTab = (
    <Card bordered={false} className="dashboard-card" title={`共收藏 ${workflows.length} 个工作流`}>
      <List
        loading={loading}
        dataSource={workflows}
        locale={{ emptyText: '你还没有收藏任何工作流。' }}
        renderItem={(workflow) => (
          <List.Item key={workflow.id}>
            <Card size="small" style={{ width: '100%', background: '#f0f5ff' }}>
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
                  <Space wrap>
                    <Link href={`/workflows/${workflow.id}`}>{workflow.name}</Link>
                    <Tag color="blue">{workflow.scenario}</Tag>
                    {workflow.featured ? <Tag color="gold">精选</Tag> : null}
                  </Space>
                  <Text type="secondary">{new Date(workflow.updatedAt).toLocaleString('zh-CN')}</Text>
                </Space>
                <Text type="secondary">创建人：{workflow.creatorName}</Text>
                <Paragraph style={{ marginBottom: 0 }}>
                  {workflow.description.slice(0, 120)}
                </Paragraph>
                <Space wrap>
                  {workflow.tools.map((tool) => (
                    <Tag key={tool.id}>{tool.name}</Tag>
                  ))}
                </Space>
                <Text type="secondary">
                  关联工具 {workflow.toolCount} 个
                </Text>
              </Space>
            </Card>
          </List.Item>
        )}
      />
    </Card>
  );

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="我的收藏"
      subtitle="集中查看经常回访的工具和工作流"
      section="favorites"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Space direction="vertical" size={10}>
          <Tag color="blue" style={{ width: 'fit-content', margin: 0 }}>Favorites</Tag>
          <Title level={2} style={{ margin: 0 }}>我的收藏</Title>
          <Space>
            <Button type="primary" style={{ background: '#183153' }}>
              <Link href="/tools">回到工具广场</Link>
            </Button>
            <Button onClick={() => void loadPage()} loading={loading}>刷新</Button>
          </Space>
        </Space>
      </Card>

      <Tabs
        defaultActiveKey="tools"
        items={[
          {
            key: 'tools',
            label: `工具 (${tools.length})`,
            children: toolTab
          },
          {
            key: 'workflows',
            label: `工作流 (${workflows.length})`,
            children: workflowTab
          }
        ]}
      />
    </EnterpriseShell>
  );
}
