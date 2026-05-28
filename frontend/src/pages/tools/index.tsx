import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import {
  App,
  Button,
  Card,
  Col,
  Input,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
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

function statusColor(status: ToolListItem['status']) {
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

export default function ToolsPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);
  const [tools, setTools] = useState<ToolListItem[]>([]);
  const [myTools, setMyTools] = useState<ToolListItem[]>([]);
  const [tags, setTags] = useState<{ id: number; name: string }[]>([]);
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([]);
  const [sortBy, setSortBy] = useState<'createdAt' | 'starCount'>('createdAt');
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');
  const isAdmin = currentUser?.roleCodes.includes('ADMIN') ?? false;
  const canReview = isAdmin || (currentUser?.roleCodes.includes('REVIEWER') ?? false);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (keyword.trim()) {
        params.set('keyword', keyword.trim());
      }
      selectedTagIds.forEach((tagId) => params.append('tagIds', String(tagId)));
      params.set('sortBy', sortBy);
      params.set('sortOrder', sortOrder);
      const query = params.toString();
      const [visibleTools, mine] = await Promise.all([
        apiRequest<ToolListItem[]>(`/api/tools${query ? `?${query}` : ''}`),
        apiRequest<ToolListItem[]>(`/api/tools?mine=true&sortBy=${sortBy}&sortOrder=${sortOrder}`),
      ]);
      setTools(visibleTools);
      setMyTools(mine);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工具失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, message, selectedTagIds, sortBy, sortOrder]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        setCurrentUser(user);
        return Promise.all([
          loadData(),
          apiRequest<{ id: number; name: string; description?: string | null }[]>('/api/tags').then(setTags)
        ]);
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

  const columns: ColumnsType<ToolListItem> = [
    {
      title: '工具名称',
      dataIndex: 'name',
      key: 'name',
      render: (_, record) => <Link href={`/tools/${record.id}`}>{record.name}</Link>
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      key: 'summary',
      render: (value?: string | null) => value || '暂无摘要'
    },
    {
      title: '推荐人',
      dataIndex: 'recommenderName',
      key: 'recommenderName',
      width: 140
    },
    {
      title: '标签',
      key: 'tags',
      width: 220,
      render: (_, record) => (
        <Space wrap>
          {record.tags.map((tag) => (
            <Tag key={tag.id}>{tag.name}</Tag>
          ))}
        </Space>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 160,
      render: (value: ToolListItem['status']) => <Tag color={statusColor(value)}>{value}</Tag>
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 220,
      render: (value: string) => new Date(value).toLocaleString('zh-CN')
    }
  ];

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在载入工具平台...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="工具目录"
      subtitle="围绕团队日常使用场景进行集中检索、提交与协作"
      section="tools"
    >
          <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
            <Row gutter={[24, 24]} align="middle">
              <Col xs={24} lg={15}>
                <Space direction="vertical" size={14}>
                  <Tag color="blue" style={{ width: 'fit-content', margin: 0 }}>Workspace</Tag>
                  <Title level={2} style={{ margin: 0 }}>快速找到能解决问题的工具</Title>
                  <Space wrap>
                    <Button type="primary" style={{ background: '#183153' }}>
                      <Link href="/tools/submit">提交新工具</Link>
                    </Button>
                    <Button>
                      <Link href="/workflows">查看工作流目录</Link>
                    </Button>
                    <Button>
                      <Link href="/tools/favorites">查看我的收藏</Link>
                    </Button>
                    {isAdmin ? (
                      <Button>
                        <Link href="/admin/tools">进入工具管理</Link>
                      </Button>
                    ) : null}
                    {canReview ? (
                      <Button>
                        <Link href="/admin/reviews">处理待审核工具</Link>
                      </Button>
                    ) : null}
                  </Space>
                </Space>
              </Col>
              <Col xs={24} lg={9}>
                <Card bordered={false} className="dashboard-card" style={{ background: '#17384b', color: '#f7f4ea' }}>
                  <Space direction="vertical" size={6}>
                    <Text style={{ color: '#d8ecf4' }}>当前数据概览</Text>
                    <Title level={3} style={{ color: '#f7f4ea', margin: 0 }}>{tools.length}</Title>
                    <Text style={{ color: 'rgba(247, 244, 234, 0.72)' }}>已发布工具</Text>
                    <Text style={{ color: '#f7f4ea' }}>{myTools.length} 个我的工具记录</Text>
                  </Space>
                </Card>
              </Col>
            </Row>
          </Card>

          <Row gutter={[24, 24]}>
            <Col xs={24} xl={16}>
              <Card bordered={false} className="dashboard-card" title="工具列表" extra={<Button onClick={() => void loadData()}>刷新</Button>}>
                <Space direction="vertical" size={16} style={{ width: '100%' }}>
                  <Input.Search
                    placeholder="按名称、摘要或描述搜索"
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    onSearch={() => void loadData()}
                    size="large"
                  />
                  <Row gutter={[12, 12]}>
                    <Col xs={24} md={14}>
                      <Select
                        mode="multiple"
                        allowClear
                        size="large"
                        placeholder="按标签筛选"
                        style={{ width: '100%' }}
                        value={selectedTagIds}
                        onChange={setSelectedTagIds}
                        options={tags.map((tag) => ({ label: tag.name, value: tag.id }))}
                      />
                    </Col>
                    <Col xs={24} md={5}>
                      <Select
                        size="large"
                        value={sortBy}
                        onChange={(value) => setSortBy(value)}
                        style={{ width: '100%' }}
                        options={[
                          { label: '按时间', value: 'createdAt' },
                          { label: '按热度', value: 'starCount' }
                        ]}
                      />
                    </Col>
                    <Col xs={24} md={5}>
                      <Select
                        size="large"
                        value={sortOrder}
                        onChange={(value) => setSortOrder(value)}
                        style={{ width: '100%' }}
                        options={[
                          { label: '降序', value: 'desc' },
                          { label: '升序', value: 'asc' }
                        ]}
                      />
                    </Col>
                  </Row>
                  <Table
                    rowKey="id"
                    columns={columns}
                    dataSource={tools}
                    loading={loading}
                    pagination={{ pageSize: 6 }}
                    scroll={{ x: 880 }}
                  />
                </Space>
              </Card>
            </Col>
            <Col xs={24} xl={8}>
              <Card bordered={false} className="dashboard-card" title="我的提交与工具">
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  {myTools.length === 0 ? (
                    <Text type="secondary">你还没有提交过工具。</Text>
                  ) : myTools.slice(0, 6).map((tool) => (
                    <Card key={tool.id} size="small" className="dashboard-card" style={{ background: '#fffdf7' }}>
                      <Space direction="vertical" size={8} style={{ width: '100%' }}>
                        <Space style={{ justifyContent: 'space-between', width: '100%' }}>
                          <Link href={`/tools/${tool.id}`}>{tool.name}</Link>
                          <Tag color={statusColor(tool.status)}>{tool.status}</Tag>
                        </Space>
                        <Space wrap>
                          {tool.tags.map((tag) => (
                            <Tag key={tag.id}>{tag.name}</Tag>
                          ))}
                        </Space>
                        <Text type="secondary">{tool.summary || tool.description.slice(0, 48)}</Text>
                      </Space>
                    </Card>
                  ))}
                </Space>
              </Card>
            </Col>
          </Row>
    </EnterpriseShell>
  );
}
