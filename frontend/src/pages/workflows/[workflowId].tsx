import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { App, Button, Card, List, Space, Tag, Typography } from 'antd';
import { StarOutlined, StarFilled, HeartOutlined, HeartFilled } from '@ant-design/icons';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Paragraph, Text } = Typography;

type WorkflowDetail = {
  id: number;
  name: string;
  scenario: string;
  description: string;
  steps: string;
  featured: boolean;
  status: string;
  creatorId: number;
  creatorName: string;
  starCount: number;
  favoriteCount: number;
  starred: boolean;
  favorited: boolean;
  tools: {
    id: number;
    name: string;
    summary?: string | null;
    description: string;
    url: string;
    tags: { id: number; name: string }[];
  }[];
  createdAt: string;
  updatedAt: string;
};

export default function WorkflowDetailPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [workflow, setWorkflow] = useState<WorkflowDetail | null>(null);
  const [booting, setBooting] = useState(true);
  const [starLoading, setStarLoading] = useState(false);
  const [favoriteLoading, setFavoriteLoading] = useState(false);

  const loadWorkflow = async (workflowId: string) => {
    const detail = await apiRequest<WorkflowDetail>(`/api/workflows/${workflowId}`);
    setWorkflow(detail);
  };

  useEffect(() => {
    const workflowId = router.query.workflowId;
    if (!router.isReady) {
      return;
    }
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }
    if (!workflowId || Array.isArray(workflowId)) {
      message.error('工作流编号无效');
      void router.replace('/workflows');
      return;
    }

    Promise.all([
      apiRequest<CurrentUser>('/api/auth/me'),
      apiRequest<WorkflowDetail>(`/api/workflows/${workflowId}`)
    ])
      .then(([user, detail]) => {
        setCurrentUser(user);
        setWorkflow(detail);
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '加载工作流详情失败');
        void router.replace('/workflows');
      })
      .finally(() => setBooting(false));
  }, [message, router, router.isReady, router.query.workflowId]);

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

  const handleStar = async () => {
    if (!workflow) return;
    setStarLoading(true);
    try {
      if (workflow.starred) {
        await apiRequest(`/api/workflows/${workflow.id}/stars`, { method: 'DELETE' });
        setWorkflow({ ...workflow, starred: false, starCount: workflow.starCount - 1 });
        message.success('已取消点赞');
      } else {
        await apiRequest(`/api/workflows/${workflow.id}/stars`, { method: 'POST' });
        setWorkflow({ ...workflow, starred: true, starCount: workflow.starCount + 1 });
        message.success('已点赞');
      }
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败');
    } finally {
      setStarLoading(false);
    }
  };

  const handleFavorite = async () => {
    if (!workflow) return;
    setFavoriteLoading(true);
    try {
      if (workflow.favorited) {
        await apiRequest(`/api/workflows/${workflow.id}/favorites`, { method: 'DELETE' });
        setWorkflow({ ...workflow, favorited: false, favoriteCount: workflow.favoriteCount - 1 });
        message.success('已取消收藏');
      } else {
        await apiRequest(`/api/workflows/${workflow.id}/favorites`, { method: 'POST' });
        setWorkflow({ ...workflow, favorited: true, favoriteCount: workflow.favoriteCount + 1 });
        message.success('已收藏');
      }
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败');
    } finally {
      setFavoriteLoading(false);
    }
  };

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载工作流详情...</Text></div>;
  }

  if (!workflow) {
    return null;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="工作流详情"
      subtitle="查看完整步骤与关联工具"
      section="workflows"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Space wrap>
            {workflow.featured ? <Tag color="gold">精选工作流</Tag> : null}
            <Tag>{workflow.scenario}</Tag>
            <Text type="secondary">创建人：{workflow.creatorName}</Text>
            <Text type="secondary">更新于 {new Date(workflow.updatedAt).toLocaleString('zh-CN')}</Text>
          </Space>
          <div>
            <Title level={2} style={{ marginBottom: 8 }}>{workflow.name}</Title>
            <Paragraph style={{ marginBottom: 0 }}>{workflow.description}</Paragraph>
          </div>
          <Space wrap>
            <Button
              icon={workflow.starred ? <StarFilled style={{ color: '#faad14' }} /> : <StarOutlined />}
              loading={starLoading}
              onClick={() => void handleStar()}
            >
              点赞 {workflow.starCount > 0 && workflow.starCount}
            </Button>
            <Button
              icon={workflow.favorited ? <HeartFilled style={{ color: '#ff4d4f' }} /> : <HeartOutlined />}
              loading={favoriteLoading}
              onClick={() => void handleFavorite()}
            >
              收藏 {workflow.favoriteCount > 0 && workflow.favoriteCount}
            </Button>
          </Space>
        </Space>
      </Card>

      <Card bordered={false} className="dashboard-card" title="执行步骤" style={{ marginBottom: 24 }}>
        <Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{workflow.steps}</Paragraph>
      </Card>

      <Card bordered={false} className="dashboard-card" title={`关联工具 (${workflow.tools.length})`}>
        <List
          dataSource={workflow.tools}
          renderItem={(tool, index) => (
            <List.Item key={tool.id}>
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                <Space wrap>
                  <Tag color="blue">步骤 {index + 1}</Tag>
                  <Link href={`/tools/${tool.id}`}>{tool.name}</Link>
                </Space>
                <Paragraph style={{ marginBottom: 0 }}>{tool.summary || tool.description}</Paragraph>
                <Space wrap>
                  {tool.tags.map((tag) => (
                    <Tag key={tag.id}>{tag.name}</Tag>
                  ))}
                </Space>
                {tool.url ? (
                  <Button type="primary" style={{ width: 'fit-content', background: '#183153' }} href={tool.url} target="_blank">
                    打开工具
                  </Button>
                ) : null}
              </Space>
            </List.Item>
          )}
        />
      </Card>
    </EnterpriseShell>
  );
}
