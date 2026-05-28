import Link from 'next/link';
import { useRouter } from 'next/router';
import { Button, Layout, Menu, Space, Typography } from 'antd';
import type { ReactNode } from 'react';
import type { CurrentUser } from '../lib/auth';

const { Header, Content } = Layout;
const { Title, Text } = Typography;

type EnterpriseShellProps = {
  children: ReactNode;
  currentUser?: CurrentUser | null;
  onLogout?: () => void | Promise<void>;
  title: string;
  subtitle?: string;
  section: string;
};

type NavItem = {
  key: string;
  href: string;
  label: string;
  adminOnly?: boolean;
  reviewAccess?: boolean;
};

const primaryNav: NavItem[] = [
  { key: 'tools', href: '/tools', label: '工具目录' },
  { key: 'submit', href: '/tools/submit', label: '提交工具' },
  { key: 'favorites', href: '/tools/favorites', label: '我的收藏' },
  { key: 'workflows', href: '/workflows', label: '工作流' }
];

const adminNav: NavItem[] = [
  { key: 'admin-tools', href: '/admin/tools', label: '工具管理', adminOnly: true },
  { key: 'admin-tags', href: '/admin/tags', label: '标签管理', adminOnly: true },
  { key: 'admin-reviews', href: '/admin/reviews', label: '审核中心', reviewAccess: true },
  { key: 'admin-users', href: '/admin/users', label: '账号权限', adminOnly: true },
  { key: 'admin-import-export', href: '/admin/import-export', label: '导入导出', adminOnly: true },
  { key: 'admin-logs', href: '/admin/logs', label: '日志审计', adminOnly: true }
];

export function EnterpriseShell({ children, currentUser, onLogout, title, subtitle, section }: EnterpriseShellProps) {
  const router = useRouter();
  const roleCodes = currentUser?.roleCodes ?? [];
  const isAdmin = roleCodes.includes('ADMIN');
  const canReview = isAdmin || roleCodes.includes('REVIEWER');
  const navItems = [...primaryNav, ...adminNav.filter((item) => {
    if (item.adminOnly) {
      return isAdmin;
    }
    if (item.reviewAccess) {
      return canReview;
    }
    return true;
  })];

  return (
    <Layout style={{ minHeight: '100vh', background: 'var(--page-bg)' }}>
      <Header
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 20,
          height: 'auto',
          padding: '14px 24px 10px',
          background: 'rgba(249, 251, 247, 0.92)',
          backdropFilter: 'blur(16px)',
          borderBottom: '1px solid var(--line)'
        }}
      >
        <div className="shell-header-top">
          <div>
            <Text className="shell-eyebrow">TOOL SHARE</Text>
            <Title level={4} style={{ margin: '2px 0 0', color: 'var(--ink)' }}>{title}</Title>
            {subtitle ? <Text type="secondary">{subtitle}</Text> : null}
          </div>
          <Space size={12} align="center">
            {currentUser ? (
              <div style={{ textAlign: 'right' }}>
                <Text strong>{currentUser.realName}</Text>
                <br />
                <Text type="secondary">{currentUser.roleCodes.join(' / ')}</Text>
              </div>
            ) : null}
            {onLogout ? <Button onClick={() => void onLogout()}>退出</Button> : null}
          </Space>
        </div>
        <Menu
          mode="horizontal"
          selectedKeys={[section]}
          style={{ marginTop: 10, borderBottom: 'none', background: 'transparent', minWidth: 0 }}
          items={navItems.map((item) => ({
            key: item.key,
            label: <Link href={item.href}>{item.label}</Link>,
            onClick: () => {
              if (router.asPath !== item.href) {
                void router.push(item.href);
              }
            }
          }))}
        />
      </Header>
      <Content style={{ padding: '28px 24px 48px' }}>
        <div style={{ maxWidth: 1280, margin: '0 auto' }}>{children}</div>
      </Content>
    </Layout>
  );
}
