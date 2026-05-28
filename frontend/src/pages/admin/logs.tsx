import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import { App, Button, Card, Input, Select, Space, Table, Tabs, Tag, Typography } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { EnterpriseShell } from '../../components/enterprise-shell';
import { apiRequest } from '../../lib/api';
import { clearAuthSession, readAuthSession, type CurrentUser } from '../../lib/auth';

const { Title, Text } = Typography;

type PagedResponse<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
};

type AuditLog = {
  id: number;
  action: string;
  objectType: string;
  objectId?: string | null;
  operatorId?: number | null;
  operatorName?: string | null;
  detail?: string | null;
  createdAt: string;
};

type OperationLog = {
  id: number;
  operation: string;
  module: string;
  userId?: number | null;
  username?: string | null;
  realName?: string | null;
  success: boolean;
  message?: string | null;
  createdAt: string;
};

export default function AdminLogsPage() {
  const router = useRouter();
  const { message } = App.useApp();
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [booting, setBooting] = useState(true);

  const [auditKeyword, setAuditKeyword] = useState('');
  const [auditAction, setAuditAction] = useState<string | undefined>();
  const [auditObjectType, setAuditObjectType] = useState<string | undefined>();
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditData, setAuditData] = useState<PagedResponse<AuditLog>>({ items: [], total: 0, page: 1, size: 10 });

  const [operationKeyword, setOperationKeyword] = useState('');
  const [operationModule, setOperationModule] = useState<string | undefined>();
  const [operationSuccess, setOperationSuccess] = useState<string | undefined>();
  const [operationLoading, setOperationLoading] = useState(false);
  const [operationData, setOperationData] = useState<PagedResponse<OperationLog>>({ items: [], total: 0, page: 1, size: 10 });

  const loadAuditLogs = useCallback(async (page = 1, size = auditData.size) => {
    setAuditLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (auditKeyword.trim()) {
        params.set('keyword', auditKeyword.trim());
      }
      if (auditAction) {
        params.set('action', auditAction);
      }
      if (auditObjectType) {
        params.set('objectType', auditObjectType);
      }
      const data = await apiRequest<PagedResponse<AuditLog>>(`/api/admin/logs/audit?${params.toString()}`);
      setAuditData(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载审计日志失败');
    } finally {
      setAuditLoading(false);
    }
  }, [auditAction, auditData.size, auditKeyword, auditObjectType, message]);

  const loadOperationLogs = useCallback(async (page = 1, size = operationData.size) => {
    setOperationLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (operationKeyword.trim()) {
        params.set('keyword', operationKeyword.trim());
      }
      if (operationModule) {
        params.set('module', operationModule);
      }
      if (operationSuccess) {
        params.set('success', operationSuccess);
      }
      const data = await apiRequest<PagedResponse<OperationLog>>(`/api/admin/logs/operations?${params.toString()}`);
      setOperationData(data);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '加载操作日志失败');
    } finally {
      setOperationLoading(false);
    }
  }, [message, operationData.size, operationKeyword, operationModule, operationSuccess]);

  useEffect(() => {
    if (!readAuthSession()) {
      void router.replace('/login');
      return;
    }

    apiRequest<CurrentUser>('/api/auth/me')
      .then((user) => {
        if (!user.roleCodes.includes('ADMIN')) {
          message.error('当前账号无权访问日志管理页');
          void router.replace('/tools');
          return;
        }
        setCurrentUser(user);
        return Promise.all([loadAuditLogs(), loadOperationLogs()]);
      })
      .catch((error) => {
        clearAuthSession();
        message.error(error instanceof Error ? error.message : '登录态已失效');
        void router.replace('/login');
      })
      .finally(() => setBooting(false));
  }, [loadAuditLogs, loadOperationLogs, message, router]);

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

  const auditColumns: ColumnsType<AuditLog> = [
    { title: '动作', dataIndex: 'action', key: 'action', width: 160 },
    { title: '对象类型', dataIndex: 'objectType', key: 'objectType', width: 120 },
    { title: '对象 ID', dataIndex: 'objectId', key: 'objectId', width: 120, render: (value?: string | null) => value || '-' },
    { title: '操作者', dataIndex: 'operatorName', key: 'operatorName', width: 140, render: (value?: string | null) => value || '-' },
    { title: '详情', dataIndex: 'detail', key: 'detail', render: (value?: string | null) => value || '暂无' },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 220 }
  ];

  const operationColumns: ColumnsType<OperationLog> = [
    { title: '模块', dataIndex: 'module', key: 'module', width: 140 },
    { title: '操作', dataIndex: 'operation', key: 'operation', width: 220 },
    {
      title: '结果',
      dataIndex: 'success',
      key: 'success',
      width: 100,
      render: (value: boolean) => <Tag color={value ? 'green' : 'volcano'}>{value ? 'SUCCESS' : 'FAILED'}</Tag>
    },
    {
      title: '操作者',
      key: 'operator',
      width: 160,
      render: (_, record) => record.realName || record.username || '-'
    },
    { title: '详情', dataIndex: 'message', key: 'message', render: (value?: string | null) => value || '暂无' },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', width: 220 }
  ];

  const handleAuditTableChange = (pagination: TablePaginationConfig) => {
    void loadAuditLogs(pagination.current ?? 1, pagination.pageSize ?? auditData.size);
  };

  const handleOperationTableChange = (pagination: TablePaginationConfig) => {
    void loadOperationLogs(pagination.current ?? 1, pagination.pageSize ?? operationData.size);
  };

  if (booting) {
    return <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><Text>正在加载日志管理页...</Text></div>;
  }

  return (
    <EnterpriseShell
      currentUser={currentUser}
      onLogout={handleLogout}
      title="日志管理"
      subtitle="统一查看审计日志与最近 200 条操作记录"
      section="admin-logs"
    >
      <Card bordered={false} className="page-hero" style={{ marginBottom: 24 }}>
        <Space direction="vertical" size={12}>
          <Tag color="blue" style={{ width: 'fit-content', margin: 0 }}>Operations Center</Tag>
          <Title level={2} style={{ margin: 0 }}>统一查看审计日志与操作日志</Title>
          <Text type="secondary">操作日志仅保留工具与工作流的提交、修改、删除相关记录，最多展示最近 200 条。</Text>
        </Space>
      </Card>

      <Tabs
        items={[
          {
            key: 'audit',
            label: '审计日志',
            children: (
              <Card bordered={false} className="dashboard-card">
                <Space wrap style={{ marginBottom: 16 }}>
                  <Input
                    placeholder="搜索操作者、对象 ID、详情"
                    value={auditKeyword}
                    onChange={(event) => setAuditKeyword(event.target.value)}
                    style={{ width: 240 }}
                  />
                  <Select
                    allowClear
                    placeholder="动作"
                    value={auditAction}
                    onChange={(value) => setAuditAction(value)}
                    style={{ width: 180 }}
                    options={[
                      { label: 'USER_CREATED', value: 'USER_CREATED' },
                      { label: 'TOOL_CREATED', value: 'TOOL_CREATED' },
                      { label: 'TAG_CREATED', value: 'TAG_CREATED' },
                      { label: 'TOOLS_IMPORTED', value: 'TOOLS_IMPORTED' },
                      { label: 'TOOLS_EXPORTED', value: 'TOOLS_EXPORTED' },
                      { label: 'LOGIN_SUCCEEDED', value: 'LOGIN_SUCCEEDED' },
                      { label: 'LOGIN_FAILED', value: 'LOGIN_FAILED' }
                    ]}
                  />
                  <Select
                    allowClear
                    placeholder="对象类型"
                    value={auditObjectType}
                    onChange={(value) => setAuditObjectType(value)}
                    style={{ width: 160 }}
                    options={[
                      { label: 'USER', value: 'USER' },
                      { label: 'TOOL', value: 'TOOL' },
                      { label: 'TAG', value: 'TAG' },
                      { label: 'WORKFLOW', value: 'WORKFLOW' },
                      { label: 'IMPORT_TASK', value: 'IMPORT_TASK' },
                      { label: 'EXPORT_TASK', value: 'EXPORT_TASK' }
                    ]}
                  />
                  <Button type="primary" style={{ background: '#183153' }} onClick={() => void loadAuditLogs(1, auditData.size)}>
                    查询
                  </Button>
                </Space>
                <Table
                  rowKey="id"
                  columns={auditColumns}
                  dataSource={auditData.items}
                  loading={auditLoading}
                  pagination={{
                    current: auditData.page,
                    pageSize: auditData.size,
                    total: auditData.total,
                    showSizeChanger: true
                  }}
                  onChange={handleAuditTableChange}
                  scroll={{ x: 1080 }}
                />
              </Card>
            )
          },
          {
            key: 'operations',
            label: '操作日志',
            children: (
              <Card bordered={false} className="dashboard-card">
                <Space wrap style={{ marginBottom: 16 }}>
                  <Input
                    placeholder="搜索操作、账号、详情"
                    value={operationKeyword}
                    onChange={(event) => setOperationKeyword(event.target.value)}
                    style={{ width: 240 }}
                  />
                  <Select
                    allowClear
                    placeholder="模块"
                    value={operationModule}
                    onChange={(value) => setOperationModule(value)}
                    style={{ width: 180 }}
                    options={[
                      { label: 'TOOL', value: 'TOOL' },
                      { label: 'WORKFLOW', value: 'WORKFLOW' }
                    ]}
                  />
                  <Select
                    allowClear
                    placeholder="结果"
                    value={operationSuccess}
                    onChange={(value) => setOperationSuccess(value)}
                    style={{ width: 140 }}
                    options={[
                      { label: '成功', value: 'true' },
                      { label: '失败', value: 'false' }
                    ]}
                  />
                  <Button type="primary" style={{ background: '#183153' }} onClick={() => void loadOperationLogs(1, operationData.size)}>
                    查询
                  </Button>
                </Space>
                <Table
                  rowKey="id"
                  columns={operationColumns}
                  dataSource={operationData.items}
                  loading={operationLoading}
                  pagination={{
                    current: operationData.page,
                    pageSize: operationData.size,
                    total: operationData.total,
                    showSizeChanger: true
                  }}
                  onChange={handleOperationTableChange}
                  scroll={{ x: 1120 }}
                />
              </Card>
            )
          }
        ]}
      />
    </EnterpriseShell>
  );
}
