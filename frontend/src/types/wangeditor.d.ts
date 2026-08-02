/**
 * wangEditor for Vue3 类型声明
 *
 * @wangeditor/editor-for-vue 的 package.json exports 字段未声明 types 条件，
 * 导致 moduleResolution: "Bundler" 下 vue-tsc 无法解析其类型。
 * 此处手动提供 ambient 声明以通过类型检查。
 */
declare module '@wangeditor/editor-for-vue' {
    import type { DefineComponent } from 'vue'

    /** 富文本编辑器组件 */
    export const Editor: DefineComponent<{
        /** v-model 绑定的 HTML 内容 */
        modelValue?: string
        /** 编辑器默认配置 */
        defaultConfig?: Record<string, unknown>
        /** 编辑器模式: 'default' | 'simple' */
        mode?: string
    }>

    /** 工具栏组件 */
    export const Toolbar: DefineComponent<{
        /** 编辑器实例 */
        editor?: unknown
        /** 工具栏模式: 'default' | 'simple' */
        mode?: string
    }>
}