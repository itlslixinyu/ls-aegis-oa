import {
  AutoComplete, Cascader, Checkbox, CheckboxGroup, ColorPicker, DatePicker, Input,
  InputNumber, InputPassword, InputTag, Mention, MonthPicker, QuarterPicker, RadioGroup,
  RangePicker, Rate, Select, Slider, Switch, Textarea, TimePicker, TreeSelect, Upload,
  WeekPicker, YearPicker,
} from '@arco-design/web-vue'
import type { Component } from 'vue'
import type { ColumnItemType } from './type'

/** 动态表单直接使用组件对象，不再依赖整套Arco全局注册。 */
const components = {
  'input': Input, 'input-password': InputPassword, 'input-number': InputNumber,
  'input-tag': InputTag, 'textarea': Textarea, 'select': Select, 'tree-select': TreeSelect,
  'radio-group': RadioGroup, 'checkbox-group': CheckboxGroup, 'checkbox': Checkbox,
  'date-picker': DatePicker, 'year-picker': YearPicker, 'quarter-picker': QuarterPicker,
  'month-picker': MonthPicker, 'week-picker': WeekPicker, 'time-picker': TimePicker,
  'range-picker': RangePicker, 'color-picker': ColorPicker, 'rate': Rate, 'switch': Switch,
  'slider': Slider, 'cascader': Cascader, 'upload': Upload, 'auto-complete': AutoComplete,
  'mention': Mention,
} satisfies Record<Exclude<ColumnItemType, 'group-title'>, Component>

export function resolveFormComponent(type?: ColumnItemType) {
  return !type || type === 'group-title' ? undefined : components[type]
}
