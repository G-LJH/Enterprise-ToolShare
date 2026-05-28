import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import {
  App,
  Button,
  Card,
  Descriptions,
  Input,
  List,
  Popconfirm,
  Space,
  Tag,
  Typography
} from 'antd';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Paragraph, Text } = Typography;

type ToolDetail = {
  id: number;
  name: string;
  summary?: string | null;
  description: string;
  url?: string | null;
  usageGuide: string;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'OFFLINE';
  recommenderId: number;
  recommenderName: string;
  starCount: number;
  favoriteCount: number;
  commentCount: number;
  starred: boolean;
  favorited: boolean;
  tags: { id: number; name: string }[];
  submission?: {
    id: number;
    submitterId: number;
    submitterName: string;
    status: string;
    remark: string;
    createdAt: string;
  } | null;
  createdAt: string;
  updatedAt: string;
};

type ToolComment = {
  id: number;
  toolId: number;
  userId: number;
  authorName: string;
  content: string;
  createdAt: string;
  deletable: boolean;
};

function statusColor(status: ToolDetail['status']) {
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

export default function ToolDetailPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [tool, setTool] = useState<ToolDetail | null>(null);
  const [comments, setComments] = useState<ToolComment[]>([]);
  const [commentContent, setCommentContent] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [booting, setBooting] = useState(true);

  const toolId = router.query.toolId;

  const loadTool = useCallback(async (id: string) => {
    const detail = await apiRequest<ToolDetail>(`/api/tools/${id}`);
    setTool(detail);
  }, []);

  const loadComments = useCallback(async (id: string) => {
    const commentList = await apiRequest<ToolComment[]>(`/api/tools/${id}/comments`);
    setComments(commentList);
  }, []);

  const loadPage = useCallback(async (id: string) => {
    const [user] = await Promise.all([
      apiRequest<CurrentUser>('/api/auth/me'),
      loadTool(id),
      loadComments(id)
    ]);
    setCurrentUser(user);
  }, [loadComments, loadTool]);

  useEffect(() => {
    if (!router.isReady) {
      return;
    }
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }
    if (!toolId || Array.isArray(toolId)) {
      message.error('工具编号无效');
      void router.replace('/tools');
      return;
    }

    loadPage(toolId)
      .catch((error) => {
        if (error instanceof Error && error.message.includes('登录')) {
          clearAuthSession();
          void router.replace('/login');
          return;
        }
        message.error(error instanceof Error ? error.message : '加载工具详情失败');
        void router.replace('/tools');
      })
      .finally(() => setBooting(false));
  }, [loadPage, message, router, router.isReady, toolId]);

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

  const refreshEngagement = useCallback(async () => {
    if (!toolId || Array.isArray(toolId)) {
      return;
    }
    await Promise.all([loadTool(toolId), loadComments(toolId)]);
  }, [loadComments, loadTool, toolId]);

  const handleToggleStar = async () => {
    if (!tool || !toolId || Array.isArray(toolId)) {
      return;
    }
    setActionLoading(true);
    try {
      await apiRequest<void>(`/api/tools/${toolId}/stars`, {
        method: tool.starred ? 'DELETE' : 'POST'
      });
      message.success(tool.starred ? '已取消点赞' : '已点赞');
      await refreshEngagement();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '点赞操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleFavorite = async () => {
    if (!tool || !toolId || Array.isArray(toolId)) {
      return;
    }
    setActionLoading(true);
    try {
      await apiRequest<void>(`/api/tools/${toolId}/favorites`, {
        method: tool.favorited ? 'DELETE' : 'POST'
      });
      message.success(tool.favorited ? '已取消收藏' : '已收藏');
      await refreshEngagement();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '收藏操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCreateComment = async () => {
    if (!toolId || Array.isArray(toolId)) {
      return;
    }
    setCommentSubmitting(true);
    try {
      await apiRequest<ToolComment>(`/api/tools/${toolId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ content: commentContent })
      });
      setCommentContent('');
      message.success('评论已发布');
      await refreshEngagement();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '评论发布失败');
    } finally {
      setCommentSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    if (!toolId || Array.isArray(toolId)) {
      return;
    }
    setActionLoading(true);
    try {
      await apiRequest<void>(`/api/tools/${toolId}/comments/${commentId}`, {
        method: 'DELETE'
      });
      message.success('评论已删除');
      await refreshEngagement();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '评论删除失败');
    } finally {
      setActionLoading(false);
    }
  };

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载工具详情...</Text></div>;
  }

  if (!tool) {
    return null;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="工具详情"
      subtitle="查看工具说明、互动反馈与提交记录"
      section="tools"
    >
          <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Space wrap>
                <Tag color={statusColor(tool.status)}>{tool.status}</Tag>
                <Text type="secondary">推荐人：{tool.recommenderName}</Text>
                <Text type="secondary">更新于 {new Date(tool.updatedAt).toLocaleString('zh-CN')}</Text>
              </Space>
              <Space wrap>
                {tool.tags.map((tag) => (
                  <Tag key={tag.id}>{tag.name}</Tag>
                ))}
              </Space>
              <div>
                <Title level={2} style={{ marginBottom: 8 }}>{tool.name}</Title>
                <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  {tool.summary || '暂无摘要'}
                </Paragraph>
              </div>
              <Space wrap>
                <Button
                  type={tool.starred ? 'default' : 'primary'}
                  ghost={!tool.starred}
                  onClick={() => void handleToggleStar()}
                  loading={actionLoading}
                >
                  {tool.starred ? `已点赞 ${tool.starCount}` : `点赞 ${tool.starCount}`}
                </Button>
                <Button
                  type={tool.favorited ? 'default' : 'primary'}
                  ghost={!tool.favorited}
                  onClick={() => void handleToggleFavorite()}
                  loading={actionLoading}
                >
                  {tool.favorited ? `已收藏 ${tool.favoriteCount}` : `收藏 ${tool.favoriteCount}`}
                </Button>
                <Button>
                  <Link href="/tools">返回列表</Link>
                </Button>
              </Space>
            </Space>
          </Card>

          <Card bordered={false} className="dashboard-card" style={{ marginBottom: 24 }}>
            <Descriptions title="基础信息" column={1} styles={{ label: { width: 120 } }}>
              <Descriptions.Item label="工具链接">{tool.url || '未提供'}</Descriptions.Item>
              <Descriptions.Item label="创建时间">{new Date(tool.createdAt).toLocaleString('zh-CN')}</Descriptions.Item>
              <Descriptions.Item label="互动计数">
                点赞 {tool.starCount} / 收藏 {tool.favoriteCount} / 评论 {tool.commentCount}
              </Descriptions.Item>
            </Descriptions>
          </Card>

          <Card bordered={false} className="dashboard-card" title="工具描述" style={{ marginBottom: 24 }}>
            <Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{tool.description}</Paragraph>
          </Card>

          <Card bordered={false} className="dashboard-card" title="使用方法" style={{ marginBottom: 24 }}>
            <Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{tool.usageGuide}</Paragraph>
          </Card>

          <Card
            bordered={false}
            className="dashboard-card"
            title={`评论区 (${tool.commentCount})`}
            extra={<Text type="secondary">支持作者本人或管理员删除评论</Text>}
            style={{ marginBottom: 24 }}
          >
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Input.TextArea
                rows={4}
                maxLength={500}
                showCount
                placeholder="写下你对这个工具的使用感受、注意事项或推荐理由"
                value={commentContent}
                onChange={(event) => setCommentContent(event.target.value)}
              />
              <Space>
                <Button
                  type="primary"
                  style={{ background: '#183153' }}
                  onClick={() => void handleCreateComment()}
                  loading={commentSubmitting}
                >
                  发布评论
                </Button>
                <Button onClick={() => void refreshEngagement()}>刷新评论</Button>
              </Space>

              <List
                dataSource={comments}
                locale={{ emptyText: '还没有评论，欢迎留下第一条反馈。' }}
                renderItem={(comment) => (
                  <List.Item
                    key={comment.id}
                    actions={comment.deletable ? [
                      <Popconfirm
                        key="delete"
                        title="确认删除这条评论？"
                        okText="删除"
                        cancelText="取消"
                        onConfirm={() => void handleDeleteComment(comment.id)}
                      >
                        <Button type="link" danger size="small" disabled={actionLoading}>删除</Button>
                      </Popconfirm>
                    ] : []}
                  >
                    <List.Item.Meta
                      title={
                        <Space wrap>
                          <Text strong>{comment.authorName}</Text>
                          <Text type="secondary">{new Date(comment.createdAt).toLocaleString('zh-CN')}</Text>
                        </Space>
                      }
                      description={<Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>{comment.content}</Paragraph>}
                    />
                  </List.Item>
                )}
              />
            </Space>
          </Card>

          {tool.submission ? (
            <Card bordered={false} className="dashboard-card" title="提交记录">
              <Descriptions column={1} styles={{ label: { width: 140 } }}>
                <Descriptions.Item label="提交人">{tool.submission.submitterName}</Descriptions.Item>
                <Descriptions.Item label="提交状态">{tool.submission.status}</Descriptions.Item>
                <Descriptions.Item label="备注">{tool.submission.remark}</Descriptions.Item>
                <Descriptions.Item label="提交时间">{new Date(tool.submission.createdAt).toLocaleString('zh-CN')}</Descriptions.Item>
              </Descriptions>
            </Card>
          ) : null}
    </EnterpriseShell>
  );
}
