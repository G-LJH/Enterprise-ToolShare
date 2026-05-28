import { App, Button, Divider, Input, Select, Space } from 'antd';
import { useState } from 'react';

type TagOption = {
  id: number;
  name: string;
};

type TagSelectorProps = {
  options: TagOption[];
  value?: number[];
  onChange: (value: number[]) => void;
  onCreateTag: (name: string) => Promise<TagOption | null>;
  placeholder?: string;
};

export function TagSelector({ options, value = [], onChange, onCreateTag, placeholder }: TagSelectorProps) {
  const { message } = App.useApp();
  const [creatingName, setCreatingName] = useState('');
  const [creating, setCreating] = useState(false);

  const handleCreate = async () => {
    const normalized = creatingName.trim();
    if (!normalized) {
      message.warning('请输入标签名称');
      return;
    }
    setCreating(true);
    try {
      const created = await onCreateTag(normalized);
      if (created && !value.includes(created.id)) {
        onChange([...value, created.id]);
      }
      setCreatingName('');
    } catch (error) {
      message.error(error instanceof Error ? error.message : '创建标签失败');
    } finally {
      setCreating(false);
    }
  };

  return (
    <Select
      mode="multiple"
      allowClear
      placeholder={placeholder}
      value={value}
      onChange={onChange}
      style={{ width: '100%' }}
      options={options.map((tag) => ({ label: tag.name, value: tag.id }))}
      dropdownRender={(menu) => (
        <>
          {menu}
          <Divider style={{ margin: '8px 0' }} />
          <Space.Compact style={{ width: '100%', padding: '0 8px 8px' }}>
            <Input
              placeholder="新建标签"
              value={creatingName}
              onChange={(event) => setCreatingName(event.target.value)}
              onPressEnter={() => void handleCreate()}
            />
            <Button type="primary" loading={creating} onClick={() => void handleCreate()}>
              添加
            </Button>
          </Space.Compact>
        </>
      )}
    />
  );
}
