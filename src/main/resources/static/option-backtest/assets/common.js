/* 期权 BS 回测 · 公共 JS（主题 / API / 图表 / 格式化） */
(function () {
    'use strict';

    const THEME_KEY = 'obt.theme';

    function currentTheme() {
        return document.documentElement.getAttribute('data-theme') || 'a';
    }

    function setTheme(t) {
        t = t === 'b' ? 'b' : 'a';
        document.documentElement.setAttribute('data-theme', t);
        try {
            localStorage.setItem(THEME_KEY, t);
        } catch (e) {
        }
        const label = document.getElementById('themeLabel');
        if (label) label.textContent = t === 'a' ? 'A 终端' : 'B 专业';
        window.dispatchEvent(new CustomEvent('obt-theme', {detail: t}));
    }

    function initTheme() {
        let t = 'a';
        try {
            t = localStorage.getItem(THEME_KEY) || 'a';
        } catch (e) {
        }
        const urlTheme = new URLSearchParams(location.search).get('theme');
        if (urlTheme === 'a' || urlTheme === 'b') t = urlTheme;
        document.documentElement.setAttribute('data-theme', t === 'b' ? 'b' : 'a');
        const btn = document.getElementById('themeBtn');
        if (btn) {
            btn.addEventListener('click', () => setTheme(currentTheme() === 'a' ? 'b' : 'a'));
        }
        const label = document.getElementById('themeLabel');
        if (label) label.textContent = currentTheme() === 'a' ? 'A 终端' : 'B 专业';
    }

    function apiUrl(path) {
        const au = window.ApiUtils;
        if (!au || typeof au.buildUrl !== 'function') {
            throw new Error('ApiUtils 未加载，请检查 ../js/config.js');
        }
        return au.buildUrl(path);
    }

    async function api(path, opts) {
        const au = window.ApiUtils;
        if (!au || typeof au.fetch !== 'function') {
            throw new Error('ApiUtils 未加载，请检查 ../js/config.js');
        }
        const res = await au.fetch(path, Object.assign({
            headers: {'Content-Type': 'application/json'}
        }, opts || {}));
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const body = await res.json();
        if (!body.success) throw new Error(body.msg || '请求失败');
        return body.data;
    }

    function qs(name, def) {
        return new URLSearchParams(location.search).get(name) || def || '';
    }

    function num(v, d) {
        if (v === null || v === undefined || v === '') return '—';
        const n = Number(v);
        if (isNaN(n)) return '—';
        return n.toLocaleString('zh-CN', {minimumFractionDigits: d || 0, maximumFractionDigits: d || 0});
    }

    function money(v, d) {
        if (v === null || v === undefined || v === '') return '—';
        return '¥' + Number(v).toLocaleString('zh-CN', {minimumFractionDigits: d || 0, maximumFractionDigits: d || 0});
    }

    function pct(v, d) {
        if (v === null || v === undefined || v === '') return '—';
        const n = Number(v);
        if (isNaN(n)) return '—';
        return (n > 0 ? '+' : '') + n.toFixed(d === undefined ? 2 : d) + '%';
    }

    function cls(v) {
        return v > 0 ? 'up' : v < 0 ? 'down' : 'flat';
    }

    function statusHtml(s) {
        const m = {
            0: ['s-queued', '排队'],
            1: ['s-running', '运行中'],
            2: ['s-success', '成功'],
            3: ['s-failed', '失败'],
            4: ['s-queued', '已取消']
        };
        const x = m[Number(s)] || ['s-queued', '未知'];
        return `<span class="status ${x[0]}"><span class="d"></span>${x[1]}</span>`;
    }

    function undName(code) {
        const m = {159915: '创业板ETF', 588000: '科创50', 510500: '中证500', 510300: '沪深300', '000852': '中证1000'};
        return m[code] || code;
    }

    function chartTheme() {
        const s = getComputedStyle(document.documentElement);
        const b = currentTheme() === 'b';
        return {
            axis: s.getPropertyValue('--chart-axis').trim() || '#8d9db5',
            grid: s.getPropertyValue('--chart-grid').trim() || 'rgba(125,145,175,.1)',
            tooltipBg: s.getPropertyValue('--chart-tooltip-bg').trim() || 'rgba(10,15,24,.95)',
            tooltipBorder: s.getPropertyValue('--chart-tooltip-border').trim() || 'rgba(255,255,255,.12)',
            tooltipText: s.getPropertyValue('--chart-tooltip-text').trim() || '#dbe6f5',
            heat: s.getPropertyValue('--heat').split(',').map(x => x.trim()),
            heatBorder: s.getPropertyValue('--heat-border').trim() || 'rgba(7,9,13,.9)',
            heatLabel: s.getPropertyValue('--heat-label').trim() || '#dbe6f5',
            strategy: b ? '#2f6feb' : '#35d0ff',
            bench: b ? 'rgba(245,158,11,.8)' : 'rgba(243,194,75,.75)',
            area0: b ? 'rgba(47,111,235,.20)' : 'rgba(53,208,255,.28)',
            up: b ? '#d92b2b' : '#ff4d4f',
            down: b ? '#0f9e4f' : '#17c964'
        };
    }

    function axis(t, extra) {
        return Object.assign({
            axisLine: {lineStyle: {color: t.grid}},
            axisLabel: {color: t.axis, fontSize: 10},
            splitLine: {lineStyle: {color: t.grid}},
            axisTick: {show: false}
        }, extra || {});
    }

    function tooltip(t, extra) {
        return Object.assign({
            backgroundColor: t.tooltipBg,
            borderColor: t.tooltipBorder,
            textStyle: {color: t.tooltipText, fontSize: 11}
        }, extra || {});
    }

    function chart(el) {
        if (!el) return null;
        if (el.__chart) el.__chart.dispose();
        el.__chart = echarts.init(el);
        window.addEventListener('resize', () => el.__chart && el.__chart.resize());
        return el.__chart;
    }

    function compactMoney(v) {
        if (v == null) return '—';
        const n = Number(v);
        if (!isFinite(n)) return '—';
        const sign = n < 0 ? '-' : '';
        const a = Math.abs(n);
        let body;
        if (a >= 1e8) body = (a / 1e8).toFixed(2).replace(/\.?0+$/, '') + '亿';
        else if (a >= 1e4) body = (a / 1e4).toFixed(2).replace(/\.?0+$/, '') + '万';
        else body = a.toLocaleString('zh-CN', {maximumFractionDigits: 2});
        return '¥' + sign + body;
    }

    function equityTooltip(params, data, t) {
        const p = params && params[0];
        if (!p) return '';
        const i = p.dataIndex;
        const r = data[i] || {};
        if (window.__obtOnHoverDate && r.tradeDate) {
            window.__obtOnHoverDate(String(r.tradeDate).slice(0, 10));
        }
        const prev = i > 0 ? data[i - 1] : null;
        const eq = Number(r.equity) || 0;
        const pnl = prev ? eq - Number(prev.equity) : null;
        const totalPnl = r.cumReturnPct == null || !isFinite(Number(r.cumReturnPct))
            ? null : eq - eq / (1 + Number(r.cumReturnPct) / 100);
        const pctOf = v => eq > 0 && v != null ? (Number(v) / eq * 100).toFixed(2) + '%' : '—';
        const plainPct = v => v == null ? '—' : Number(v).toFixed(2) + '%';
        const row = (label, value, color, labelColor) => `<span style="color:${labelColor || t.axis};white-space:nowrap;font-size:10px">${label}</span><b style="font-family:var(--mono);font-weight:600;white-space:nowrap;font-size:10px;color:${color || t.tooltipText}">${value}</b>`;
        return `<div style="display:inline-block;padding:6px 9px;font-size:10px;line-height:1.5;color:${t.tooltipText}">
            <div style="font-weight:700;margin-bottom:2px;white-space:nowrap;font-size:10.5px">${r.tradeDate || ''}</div>
            <div style="display:inline-grid;grid-template-columns:auto auto;column-gap:7px;row-gap:0;justify-content:start;align-items:baseline">
                ${row('策略净值/基准', `${num(r.nav, 4)} / ${r.benchmarkNav == null ? '—' : num(r.benchmarkNav, 4)}`, t.strategy)}
                ${row('总资金', compactMoney(r.equity))}
                ${row('认购/仓位', `${compactMoney(r.callMarketValue)} / ${pctOf(r.callMarketValue)}`, t.up, t.up)}
                ${row('认沽/仓位', `${compactMoney(r.putMarketValue)} / ${pctOf(r.putMarketValue)}`, t.down, t.down)}
                ${row('市值/仓位', `${compactMoney(r.marketValue)} / ${pctOf(r.marketValue)}`)}
                ${row('可用/仓位', `${compactMoney(r.cash)} / ${pctOf(r.cash)}`)}
                ${row('买入/仓位', `${compactMoney(r.buyAmount)} / ${pctOf(r.buyAmount)}`, t.up)}
                ${row('卖出/仓位', `${compactMoney(r.sellAmount)} / ${pctOf(r.sellAmount)}`, t.down)}
                ${row('日盈亏额/日收益率', `${pnl == null ? '—' : compactMoney(pnl)} / ${r.dailyReturnPct == null ? '—' : plainPct(r.dailyReturnPct)}`, pnl == null ? t.tooltipText : pnl >= 0 ? t.up : t.down)}
                ${row('总盈亏额/总收益率', `${totalPnl == null ? '—' : compactMoney(totalPnl)} / ${r.cumReturnPct == null ? '—' : plainPct(r.cumReturnPct)}`, Number(r.cumReturnPct) >= 0 ? t.up : t.down)}
            </div>
        </div>`;
    }

    function renderEquity(el, rows) {
        const c = chart(el), t = chartTheme();
        const data = rows || [];
        c.setOption({
            animationDuration: 500,
            grid: {left: 56, right: 20, top: 34, bottom: 42},
            tooltip: tooltip(t, {
                trigger: 'axis',
                appendToBody: true,
                extraCssText: 'z-index: 9999; padding: 0; border-radius: 8px; box-shadow: 0 8px 24px rgba(0,0,0,.18);',
                formatter: params => equityTooltip(params, data, t)
            }),
            legend: {
                top: 4,
                right: 8,
                icon: 'roundRect',
                itemWidth: 10,
                itemHeight: 3,
                textStyle: {color: t.axis, fontSize: 10}
            },
            xAxis: axis(t, {type: 'category', boundaryGap: false, data: data.map(x => x.tradeDate)}),
            yAxis: axis(t, {type: 'value', scale: true}),
            dataZoom: [{type: 'inside'}, {
                type: 'slider',
                height: 14,
                bottom: 8,
                borderColor: 'transparent',
                textStyle: {color: t.axis, fontSize: 9}
            }],
            series: [
                {
                    name: '策略净值', type: 'line', data: data.map(x => Number(x.nav)), showSymbol: false,
                    lineStyle: {width: 2, color: t.strategy},
                    areaStyle: {
                        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                            {offset: 0, color: t.area0}, {offset: 1, color: 'rgba(53,208,255,0)'}
                        ])
                    }
                },
                {
                    name: '沪深300 基准', type: 'line', data: data.map(x => x.benchmarkNav), showSymbol: false,
                    lineStyle: {width: 1.2, color: t.bench, type: 'dashed'}
                }
            ]
        });
    }

    function renderDrawdown(el, rows) {
        const c = chart(el), t = chartTheme();
        c.setOption({
            animationDuration: 500,
            grid: {left: 52, right: 20, top: 22, bottom: 30},
            tooltip: tooltip(t, {trigger: 'axis', valueFormatter: v => Number(v).toFixed(2) + '%'}),
            xAxis: axis(t, {type: 'category', boundaryGap: false, data: (rows || []).map(x => x.tradeDate)}),
            yAxis: axis(t, {type: 'value', max: 0, axisLabel: {color: t.axis, fontSize: 10, formatter: '{value}%'}}),
            series: [{
                type: 'line', showSymbol: false,
                data: (rows || []).map(x => Number(x.drawdownPct)),
                lineStyle: {width: 1.2, color: t.up},
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        {offset: 0, color: 'rgba(255,77,79,.05)'}, {offset: 1, color: 'rgba(255,77,79,.35)'}
                    ])
                }
            }]
        });
    }

    function renderDaily(el, rows) {
        const c = chart(el), t = chartTheme();
        const data = (rows || []).slice(-260);
        c.setOption({
            animationDuration: 500,
            grid: {left: 52, right: 20, top: 22, bottom: 30},
            tooltip: tooltip(t, {trigger: 'axis', valueFormatter: v => Number(v).toFixed(2) + '%'}),
            xAxis: axis(t, {type: 'category', boundaryGap: false, data: data.map(x => x.tradeDate)}),
            yAxis: axis(t, {type: 'value', axisLabel: {color: t.axis, fontSize: 10, formatter: '{value}%'}}),
            series: [{
                type: 'bar', data: data.map(x => ({
                    value: Number(x.dailyReturnPct),
                    itemStyle: {color: Number(x.dailyReturnPct) >= 0 ? t.up : t.down, opacity: .8}
                }))
            }]
        });
    }

    function renderMonthly(el, monthly) {
        const c = chart(el), t = chartTheme();
        const years = Array.from(new Set(Object.keys(monthly || {}).map(k => k.slice(0, 4)))).sort();
        const months = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'];
        const data = [];
        let min = 0, max = 0;
        years.forEach((y, yi) => months.forEach((m, mi) => {
            const v = monthly[y + '-' + m];
            if (v !== undefined) {
                data.push([mi, yi, Number(v).toFixed(2)]);
                min = Math.min(min, Number(v));
                max = Math.max(max, Number(v));
            }
        }));
        c.setOption({
            animationDuration: 500,
            grid: {left: 44, right: 16, top: 8, bottom: 48},
            tooltip: tooltip(t, {formatter: p => years[p.value[1]] + '-' + months[p.value[0]] + '　' + p.value[2] + '%'}),
            xAxis: {type: 'category', data: months, axisLabel: {color: t.axis, fontSize: 9}, axisLine: {show: false}},
            yAxis: {type: 'category', data: years, axisLabel: {color: t.axis, fontSize: 9}, axisLine: {show: false}},
            visualMap: {
                min: Math.floor(min), max: Math.ceil(max), orient: 'horizontal', left: 'center', bottom: 2,
                itemWidth: 10, itemHeight: 60, precision: 0, textStyle: {color: t.axis, fontSize: 9},
                inRange: {color: t.heat}
            },
            series: [{
                type: 'heatmap', data,
                label: {show: true, fontSize: 8, color: t.heatLabel, formatter: p => p.value[2]},
                itemStyle: {borderColor: t.heatBorder, borderWidth: 1}
            }]
        });
    }

    function renderSellReasons(el, rows) {
        const c = chart(el), t = chartTheme();
        const data = (rows || []).slice(0, 9).reverse();
        c.setOption({
            animationDuration: 500,
            grid: {left: 128, right: 42, top: 16, bottom: 14},
            tooltip: tooltip(t, {trigger: 'axis', axisPointer: {type: 'shadow'}}),
            xAxis: axis(t, {type: 'value'}),
            yAxis: axis(t, {
                type: 'category',
                data: data.map(x => x.reasonDesc || x.reasonCode),
                axisLabel: {color: t.axis, fontSize: 9}
            }),
            series: [{
                type: 'bar', barWidth: 9, data: data.map(x => Number(x.cnt)),
                itemStyle: {borderRadius: [0, 3, 3, 0], color: t.strategy},
                label: {show: true, position: 'right', color: t.axis, fontSize: 9}
            }]
        });
    }

    function renderContribution(el, rows) {
        const c = chart(el), t = chartTheme();
        const data = (rows || []).map(x => ({name: undName(x.underlyingCode), value: Math.abs(Number(x.pnl || 0))}));
        c.setOption({
            animationDuration: 500,
            tooltip: tooltip(t, {trigger: 'item', formatter: '{b}<br/>收益贡献 ¥{c}'}),
            legend: {
                orient: 'vertical',
                right: 8,
                top: 'center',
                icon: 'circle',
                itemWidth: 8,
                textStyle: {color: t.axis, fontSize: 10}
            },
            series: [{
                type: 'pie', radius: ['48%', '72%'], center: ['38%', '50%'],
                itemStyle: {borderColor: t.heatBorder, borderWidth: 2},
                label: {show: false}, data,
                color: [t.strategy, '#8b7bff', '#f3c24b', '#ff8a65', '#17c964']
            }]
        });
    }

    /**
     * 企业级分页：首/尾页、上一页/下一页、页码窗口 + 省略号、每页条数、跳页。
     * opts: {total, page, size, sizes, onPage, onSize}
     */
    function renderPager(el, opts) {
        if (!el) return;
        const o = opts || {};
        const total = Math.max(0, Number(o.total) || 0);
        const sizes = o.sizes || [10, 20, 50, 100];
        const size = sizes.includes(Number(o.size)) ? Number(o.size) : sizes[0];
        const pages = Math.max(1, Math.ceil(total / size));
        const page = Math.min(Math.max(1, Number(o.page) || 1), pages);
        const from = total ? (page - 1) * size + 1 : 0;
        const to = total ? Math.min(page * size, total) : 0;

        const nums = [];
        if (pages <= 7) {
            for (let i = 1; i <= pages; i++) nums.push(i);
        } else {
            nums.push(1);
            const start = Math.max(2, page - 1);
            const end = Math.min(pages - 1, page + 1);
            if (start > 2) nums.push('...');
            for (let i = start; i <= end; i++) nums.push(i);
            if (end < pages - 1) nums.push('...');
            nums.push(pages);
        }

        const btn = (p, label, dis, title, cls) =>
            `<button type="button" class="pg ${cls || ''}" data-p="${p}" ${dis ? 'disabled' : ''} title="${title || ''}">${label}</button>`;

        el.innerHTML = `
            <div class="info">共 <b>${total.toLocaleString('zh-CN')}</b> 条${total ? ` · 当前 <b>${from}-${to}</b>` : ''}</div>
            <div class="controls">
                <label class="size-label">每页
                    <select class="page-size">${sizes.map(s => `<option value="${s}" ${s === size ? 'selected' : ''}>${s}</option>`).join('')}</select>
                    条
                </label>
                <div class="pages">
                    ${btn(1, '«', page === 1, '第一页')}
                    ${btn(page - 1, '‹', page === 1, '上一页')}
                    ${nums.map(n => n === '...'
            ? '<span class="pg-ellipsis">…</span>'
            : `<button type="button" class="pg ${n === page ? 'active' : ''}" data-p="${n}">${n}</button>`).join('')}
                    ${btn(page + 1, '›', page === pages, '下一页')}
                    ${btn(pages, '»', page === pages, '最后一页')}
                </div>
                <label class="jump-label">跳至
                    <input class="page-jump" type="number" min="1" max="${pages}" value="${page}" aria-label="跳转页码">
                    页
                </label>
            </div>`;

        const go = p => {
            const next = Math.min(Math.max(1, Number(p) || 1), pages);
            if (next !== page && typeof o.onPage === 'function') o.onPage(next);
        };
        el.querySelectorAll('[data-p]').forEach(b => b.addEventListener('click', () => go(b.dataset.p)));
        const sel = el.querySelector('.page-size');
        if (sel) sel.addEventListener('change', e => {
            if (typeof o.onSize === 'function') o.onSize(Number(e.target.value));
        });
        const jump = el.querySelector('.page-jump');
        if (jump) {
            const submit = () => go(jump.value);
            jump.addEventListener('keydown', e => {
                if (e.key === 'Enter') submit();
            });
            jump.addEventListener('change', submit);
        }
    }

    function csvDownload(filename, text) {
        const blob = new Blob(['\ufeff' + text], {type: 'text/csv;charset=utf-8'});
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename;
        a.click();
        URL.revokeObjectURL(a.href);
    }

    function toast(msg) {
        let t = document.getElementById('toast');
        if (!t) {
            t = document.createElement('div');
            t.id = 'toast';
            t.className = 'toast';
            document.body.appendChild(t);
        }
        t.textContent = msg;
        t.classList.add('show');
        clearTimeout(window.__obtToast);
        window.__obtToast = setTimeout(() => t.classList.remove('show'), 2400);
    }

    window.Obt = {
        api, apiUrl, qs, num, money, pct, cls, statusHtml, undName,
        initTheme, setTheme, currentTheme, chartTheme,
        renderEquity, renderDrawdown, renderDaily, renderMonthly, renderSellReasons, renderContribution,
        renderPager, csvDownload, toast
    };

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initTheme);
    else initTheme();
})();
