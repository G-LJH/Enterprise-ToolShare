import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { App, Button, Card, Col, Input, Modal, Row, Select, Space, Switch, Table, Tabs, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Text } = Typography;
const { TextArea } = Input;

type ToolReview = {
  submissionId: number;
  toolId: number;
  submitterId: number;
  submitterName: string;
  toolName: string;
  toolSummary?: string | null;
  toolStatus: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'OFFLINE';
  submissionStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  remark?: string | null;
  tags: { id: number; name: string }[];
  createdAt: string;
  updatedAt: string;
};

type WorkflowReview = {
  submissionId: number;
  workflowId: number;
  workflowName: string;
  workflowScenario: string;
  workflowDescription: string;
  workflowSteps: string;
  workflowFeatured: boolean;
  workflowStarCount: number;
  workflowFavoriteCount: number;
  submitterId: number;
  submissionStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  remark?: string | null;
  createdAt: string;
  updatedAt: string;
};

type ToolReviewConfig = {
  toolReviewEnabled: boolean;
};

function toolStatusColor(status: ToolReview['toolStatus']) {
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

function reviewStatusColor(status: ToolReview['submissionStatus'] | WorkflowReview['submissionStatus']) {
  if (status === 'APPROVED') {
    return 'green';
  }
  if (status === 'REJECTED') {
    return 'red';
  }
  return 'gold';
}

export default function AdminReviewsPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [toolReviews, setToolReviews] = useState<ToolReview[]>([]);
  const [workflowReviews, setWorkflowReviews] = useState<WorkflowReview[]>([]);
  const [activeTab, setActiveTab] = useState<'tools' | 'workflows'>('tools');
  const [statusFilter, setStatusFilter] = useState<'PENDING' | 'APPROVED' | 'REJECTED'>('PENDING');
  const [loading, setLoading] = useState(false);
  const [booting, setBooting] = useState(true);
  const [rejectingToolReview, setRejectingToolReview] = useState<ToolReview | null>(null);
  const [rejectingWorkflowReview, setRejectingWorkflowReview] = useState<WorkflowReview | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [decisionLoading, setDecisionLoading] = useState(false);
  const [reviewConfig, setReviewConfig] = useState<ToolReviewConfig | null>(null);
  const [configSaving, setConfigSaving] = useState(false);
  const isAdmin = currentUser?.roleCodes.includes('ADMIN') ?? false;
  const canReview = isAdmin || (currentUser?.roleCodes.includes('REVIEWER') ?? false);

  const loadToolReviews = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiRequest<ToolReview[]>(`/api/admin/reviews?status=${statusFilter}`);
      setToolReviews(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工具审核列表失败');
    } finally {
      setLoading(false);
    }
  }, [message, statusFilter]);

  const loadWorkflowReviews = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiRequest<WorkflowReview[]>(`/api/admin/workflow-reviews?status=${statusFilter}`);
      setWorkflowReviews(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载工作流审核列表失败');
    } finally {
      setLoading(false);
    }
  }, [message, statusFilter]);

  const loadReviews = useCallback(() => {
    if (activeTab === 'tools') {
      void loadToolReviews();
    } else {
      void loadWorkflowReviews();
    }
  }, [activeTab, loadToolReviews, loadWorkflowReviews]);

  const loadReviewConfig = useCallback(async () => {
    try {
      const data = await apiRequest<ToolReviewConfig>('/api/admin/reviews/config/tool-review');
      setReviewConfig(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载审核开关失败');
      setReviewConfig(null);
    }
  }, [message]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        if (!user.roleCodes.includes('ADMIN') && !user.roleCodes.includes('REVIEWER')) {
          message.error('当前账号无权访问审核管理页');
          void router.replace('/tools');
          return;
        }
        setCurrentUser(user);
        return Promise.all([loadToolReviews(), loadReviewConfig()]);
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadToolReviews, loadReviewConfig, message, router]);

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

  const handleApproveTool = async (review: ToolReview) => {
    setDecisionLoading(true);
    try {
      await apiRequest(`/api/admin/reviews/${review.submissionId}/approve`, {
        method: 'POST',
        body: JSON.stringify({ remark: '审核通过' })
      });
      message.success('审核已通过');
      await loadToolReviews();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '审核通过失败');
    } finally {
      setDecisionLoading(false);
    }
  };

  const handleApproveWorkflow = async (review: WorkflowReview) => {
    setDecisionLoading(true);
    try {
      await apiRequest(`/api/admin/workflow-reviews/${review.submissionId}/approve`, {
        method: 'POST',
        body: JSON.stringify({ remark: '审核通过' })
      });
      message.success('审核已通过');
      await loadWorkflowReviews();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '审核通过失败');
    } finally {
      setDecisionLoading(false);
    }
  };

  const handleRejectTool = async () => {
    if (!rejectingToolReview) {
      return;
    }
    setDecisionLoading(true);
    try {
      await apiRequest(`/api/admin/reviews/${rejectingToolReview.submissionId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ remark: rejectReason })
      });
      message.success('审核已驳回');
      setRejectReason('');
      setRejectingToolReview(null);
      await loadToolReviews();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '审核驳回失败');
    } finally {
      setDecisionLoading(false);
    }
  };

  const handleRejectWorkflow = async () => {
    if (!rejectingWorkflowReview) {
      return;
    }
    setDecisionLoading(true);
    try {
      await apiRequest(`/api/admin/workflow-reviews/${rejectingWorkflowReview.submissionId}/reject`, {
        method: 'POST',
        body: JSON.stringify({ remark: rejectReason })
      });
      message.success('审核已驳回');
      setRejectReason('');
      setRejectingWorkflowReview(null);
      await loadWorkflowReviews();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '审核驳回失败');
    } finally {
      setDecisionLoading(false);
    }
  };

  const handleToggleReview = async (checked: boolean) => {
    if (!isAdmin) {
      message.error('只有管理员可以修改审核开关');
      return;
    }
    setConfigSaving(true);
    try {
      const updated = await apiRequest<ToolReviewConfig>('/api/admin/reviews/config/tool-review', {
        method: 'PUT',
        body: JSON.stringify({ toolReviewEnabled: checked })
      });
      setReviewConfig(updated);
      message.success(checked ? '已开启人工审核' : '已关闭人工审核，新提交将自动通过');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '更新审核开关失败');
    } finally {
      setConfigSaving(false);
    }
  };

  const toolColumns: ColumnsType<ToolReview> = [
    {
      title: '工具',
      key: 'tool',
      render: (_, record) => (
        <Space direction="vertical" size={4}>
          <Link href={`/tools/${record.toolId}`}>{record.toolName}</Link>
          <Text type="secondary">{record.toolSummary || '暂无摘要'}</Text>
        </Space>
      )
    },
    {
      title: '提交人',
      dataIndex: 'submitterName',
      key: 'submitterName',
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
      title: '工具状态',
      dataIndex: 'toolStatus',
      key: 'toolStatus',
      width: 140,
      render: (value: ToolReview['toolStatus']) => <Tag color={toolStatusColor(value)}>{value}</Tag>
    },
    {
      title: '审核状态',
      dataIndex: 'submissionStatus',
      key: 'submissionStatus',
      width: 140,
      render: (value: ToolReview['submissionStatus']) => <Tag color={reviewStatusColor(value)}>{value}</Tag>
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      width: 220,
      render: (value?: string | null) => value || '暂无'
    },
    {
      title: '提交时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 200,
      render: (value: string) => new Date(value).toLocaleString('zh-CN')
    },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      render: (_, record) => (
        record.submissionStatus === 'PENDING' ? (
          <Space wrap>
            <Button size="small" type="primary" style={{ background: '#183153' }} loading={decisionLoading} onClick={() => void handleApproveTool(record)}>
              通过
            </Button>
            <Button size="small" danger loading={decisionLoading} onClick={() => setRejectingToolReview(record)}>
              驳回
            </Button>
          </Space>
        ) : <Text type="secondary">已完成</Text>
      )
    }
  ];

  const workflowColumns: ColumnsType<WorkflowReview> = [
    {
      title: '工作流',
      key: 'workflow',
      render: (_, record) => (
        <Space direction="vertical" size={4}>
          <Link href={`/workflows/${record.workflowId}`}>{record.workflowName}</Link>
          <Text type="secondary">{record.workflowScenario}</Text>
        </Space>
      )
    },
    {
      title: '提交人',
      key: 'submitter',
      width: 140,
      render: (_, record) => `用户 #${record.submitterId}`
    },
    {
      title: '点赞/收藏',
      key: 'engagement',
      width: 120,
      render: (_, record) => (
        <Space>
          <Tag color="blue">{record.workflowStarCount} 点赞</Tag>
          <Tag color="orange">{record.workflowFavoriteCount} 收藏</Tag>
        </Space>
      )
    },
    {
      title: '审核状态',
      dataIndex: 'submissionStatus',
      key: 'submissionStatus',
      width: 140,
      render: (value: WorkflowReview['submissionStatus']) => <Tag color={reviewStatusColor(value)}>{value}</Tag>
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      width: 220,
      render: (value?: string | null) => value || '暂无'
    },
    {
      title: '提交时间',
      key: 'createdAt',
      width: 200,
      render: (_, record) => new Date(record.createdAt).toLocaleString('zh-CN')
    },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      render: (_, record) => (
        record.submissionStatus === 'PENDING' ? (
          <Space wrap>
            <Button size="small" type="primary" style={{ background: '#183153' }} loading={decisionLoading} onClick={() => void handleApproveWorkflow(record)}>
              通过
            </Button>
            <Button size="small" danger loading={decisionLoading} onClick={() => setRejectingWorkflowReview(record)}>
              驳回
            </Button>
          </Space>
        ) : <Text type="secondary">已完成</Text>
      )
    }
  ];

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载审核台...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="审核中心"
      subtitle="处理工具和工作流审核，管理员可在此维护审核开关"
      section="admin-reviews"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} md={14}>
            <Title level={2} style={{ marginTop: 0 }}>审核发布工作台</Title>
          </Col>
          <Col xs={24} md={10}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text strong>工具审核开关</Text>
              <Space wrap align="center">
                <Switch
                  checked={reviewConfig?.toolReviewEnabled}
                  checkedChildren="开启"
                  unCheckedChildren="关闭"
                  disabled={!isAdmin}
                  loading={configSaving}
                  onChange={(checked) => void handleToggleReview(checked)}
                />
                <Text type="secondary">
                  {reviewConfig?.toolReviewEnabled ? '开启后新提交进入待审核' : '关闭后新提交直接通过'}
                </Text>
              </Space>
              {!isAdmin && canReview ? <Text type="secondary">审核员可查看当前状态，只有管理员可以修改。</Text> : null}
            </Space>
          </Col>
        </Row>
      </Card>

      <Card bordered={false} className="dashboard-card">
        <Tabs
          activeKey={activeTab}
          onChange={(key) => {
            setActiveTab(key as 'tools' | 'workflows');
            setStatusFilter('PENDING');
          }}
          items={[
            { key: 'tools', label: '工具审核' },
            { key: 'workflows', label: '工作流审核' }
          ]}
        />

        <Space direction="vertical" size={16} style={{ width: '100%', marginTop: 16 }}>
          <Row gutter={[12, 12]}>
            <Col xs={24} md={8}>
              <Select
                value={statusFilter}
                style={{ width: '100%' }}
                onChange={(value) => setStatusFilter(value)}
                options={[
                  { label: '待审核', value: 'PENDING' },
                  { label: '已通过', value: 'APPROVED' },
                  { label: '已驳回', value: 'REJECTED' }
                ]}
              />
            </Col>
            <Col xs={24} md={16}>
              <Space style={{ width: '100%', justifyContent: 'flex-end' }} wrap>
                <Button onClick={() => void loadReviews()}>刷新</Button>
                <Button>
                  <Link href="/admin/tools">进入工具管理</Link>
                </Button>
              </Space>
            </Col>
          </Row>

          <Table
            rowKey="submissionId"
            columns={activeTab === 'tools' ? (toolColumns as ColumnsType<any>) : (workflowColumns as ColumnsType<any>)}
            dataSource={activeTab === 'tools' ? toolReviews : workflowReviews}
            loading={loading}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 1220 }}
          />
        </Space>
      </Card>

      <Modal
        title={rejectingToolReview ? `驳回 ${rejectingToolReview.toolName}` : '驳回审核'}
        open={Boolean(rejectingToolReview)}
        onCancel={() => {
          setRejectingToolReview(null);
          setRejectReason('');
        }}
        onOk={() => void handleRejectTool()}
        confirmLoading={decisionLoading}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text>请填写明确的驳回原因，提交人会在提交记录中看到这条备注。</Text>
          <TextArea
            rows={4}
            maxLength={500}
            showCount
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
            placeholder="例如：工具描述不足、链接不可访问、使用方法不完整"
          />
        </Space>
      </Modal>

      <Modal
        title={rejectingWorkflowReview ? `驳回 ${rejectingWorkflowReview.workflowName}` : '驳回审核'}
        open={Boolean(rejectingWorkflowReview)}
        onCancel={() => {
          setRejectingWorkflowReview(null);
          setRejectReason('');
        }}
        onOk={() => void handleRejectWorkflow()}
        confirmLoading={decisionLoading}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text>请填写明确的驳回原因，提交人会在提交记录中看到这条备注。</Text>
          <TextArea
            rows={4}
            maxLength={500}
            showCount
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
            placeholder="例如：工作流描述不足、步骤不清晰、工具关联不合理"
          />
        </Space>
      </Modal>
    </EnterpriseShell>
  );
}
