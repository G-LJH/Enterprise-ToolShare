const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  experimental: {
    esmExternals: 'loose'
  },
  transpilePackages: [
    'antd',
    '@ant-design/cssinjs',
    '@ant-design/icons',
    'rc-util',
    'rc-field-form',
    'rc-table',
    'rc-pagination',
    'rc-picker',
    'rc-select',
    'rc-tree',
    'rc-menu',
    'rc-motion',
    'rc-dialog',
    'rc-drawer',
    'rc-collapse',
    'rc-tooltip',
    'rc-trigger',
    'rc-notification',
    'rc-tabs',
    'rc-upload',
    'rc-textarea',
    'rc-input'
  ]
};

export default nextConfig;
