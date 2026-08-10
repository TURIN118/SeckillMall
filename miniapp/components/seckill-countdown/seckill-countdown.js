// components/seckill-countdown/seckill-countdown.js
// 秒杀倒计时组件：基于服务器时间对齐的倒计时
//
// 对齐：
//   - .codeartsdoer/specs/seckill/spec.md 3.3 节
//   - .codeartsdoer/specs/seckill/design.md 4 节
//   - tasks.md S2
//
// 关键点：
//   1. 服务器时间对齐：复用 utils/time-sync.js（syncServerTime/getServerNow）
//   2. setInterval 1000ms tick，now = getServerNow()，避免客户端时钟偏差
//   3. 状态机：BEFORE（未开始）/ RUNNING（进行中）/ ENDED（已结束）
//   4. status 变化时 triggerEvent('statuschange', { status })，父级据以切换按钮
//   5. detached 清理定时器，避免内存泄漏
//   6. 时间戳容错：秒级（< 1e12）自动 ×1000 转毫秒

const { syncServerTime, getServerNow } = require('../../utils/time-sync')

Component({
    options: {
        multipleSlots: false
    },

    properties: {
        // 开始时间戳（毫秒；若传入秒级会自动 ×1000）
        startTime: {
            type: null, // 接受 string | number
            value: 0
        },
        // 结束时间戳（毫秒）
        endTime: {
            type: null,
            value: 0
        },
        // 服务器当前时间（毫秒，可选；传入则同步到 time-sync）
        serverTime: {
            type: null,
            value: 0
        },
        // 自定义文案
        beforeText: {
            type: String,
            value: '即将开抢'
        },
        runningText: {
            type: String,
            value: '距结束'
        },
        endedText: {
            type: String,
            value: '已结束'
        }
    },

    data: {
        // 状态：BEFORE | RUNNING | ENDED
        status: 'BEFORE',
        // 剩余时间分解
        remain: {
            days: 0,
            hours: 0,
            minutes: 0,
            seconds: 0,
            total: 0
        },
        // 是否显示天（days > 0）
        showDays: false,
        // 当前文案
        prefixText: '即将开抢'
    },

    // 内部定时器引用（非 data，不参与渲染）
    // this._timer = null

    lifetimes: {
        /**
         * attached：初始化时间偏移 + 启动 tick
         */
        attached() {
            // 若 serverTime 传入，则同步到 time-sync（全局对齐）
            const st = this.properties.serverTime
            if (st != null && st !== 0) {
                syncServerTime(this._normalizeTime(st))
            }
            // 立即跑一次，避免首帧空显示
            this._tick()
            // 启动每秒定时器
            this._startTimer()
        },

        /**
         * detached：清理定时器，避免内存泄漏
         */
        detached() {
            this._clearTimer()
        }
    },


    observers: {
        /**
         * serverTime 变化时重新同步
         */
        'serverTime': function (val) {
            if (val != null && val !== 0) {
                syncServerTime(this._normalizeTime(val))
                this._tick()
            }
        },
        /**
         * startTime/endTime 变化时立即重算一次
         */
        'startTime, endTime': function () {
            this._tick()
        }
    },

    methods: {
        /**
         * 启动 1s 定时器
         */
        _startTimer() {
            this._clearTimer()
            this._timer = setInterval(() => {
                this._tick()
            }, 1000)
        },

        /**
         * 清理定时器
         */
        _clearTimer() {
            if (this._timer) {
                clearInterval(this._timer)
                this._timer = null
            }
        },

        /**
         * 时间戳归一化为毫秒
         * 启发式：< 1e12 视为秒级，×1000
         * @param {string|number} t
         * @returns {number} 毫秒时间戳
         */
        _normalizeTime(t) {
            if (t == null || t === '') return 0
            const n = typeof t === 'number' ? t : Number(t)
            if (!Number.isFinite(n)) return 0
            // 1e11 ≈ 1973 年毫秒，1e12 ≈ 2001 年毫秒，1e10 ≈ 2286 年秒
            // 秒级时间戳通常 10 位，毫秒级 13 位
            if (n > 0 && n < 1e12) {
                return n * 1000
            }
            return n
        },

        /**
         * 一次 tick：计算 status + remain，必要时触发 statuschange
         */
        _tick() {
            const startMs = this._normalizeTime(this.properties.startTime)
            const endMs = this._normalizeTime(this.properties.endTime)
            const now = getServerNow()

            let status = 'BEFORE'
            let target = startMs

            if (now < startMs) {
                // 未开始：倒计时到 startTime
                status = 'BEFORE'
                target = startMs
            } else if (now >= endMs) {
                // 已结束
                status = 'ENDED'
                target = endMs
            } else {
                // 进行中：倒计时到 endTime
                status = 'RUNNING'
                target = endMs
            }

            // 计算剩余
            let total = target - now
            if (total < 0) total = 0
            const remain = this._splitRemain(total)

            // 选择文案
            let prefixText = this.properties.beforeText
            if (status === 'RUNNING') {
                prefixText = this.properties.runningText
            } else if (status === 'ENDED') {
                prefixText = this.properties.endedText
            }

            // 状态变化触发事件
            const prevStatus = this.data.status
            const patch = {
                status: status,
                remain: remain,
                showDays: remain.days > 0,
                prefixText: prefixText
            }
            this.setData(patch)

            if (prevStatus !== status) {
                this.triggerEvent('statuschange', { status: status })
            }
        },

        /**
         * 将毫秒拆分为天/时/分/秒
         * @param {number} ms
         * @returns {{days:number,hours:number,minutes:number,seconds:number,total:number}}
         */
        _splitRemain(ms) {
            const total = Math.floor(ms / 1000)
            const days = Math.floor(total / 86400)
            const hours = Math.floor((total % 86400) / 3600)
            const minutes = Math.floor((total % 3600) / 60)
            const seconds = total % 60
            return {
                days: days,
                hours: hours,
                minutes: minutes,
                seconds: seconds,
                total: total
            }
        }
    }
})