' Botones en forma de píldora en una fila. Izquierda / derecha mueve el foco; OK avisa en "itemSelected".

sub init()
    m.strip = m.top.findNode("strip")
    m.pills = []
    m.idx = 0
    m.top.observeField("focusedChild", "onFocus")
end sub

sub build()
    m.strip.removeChildrenIndex(m.strip.getChildCount(), 0)
    m.pills = []
    c = m.top.content
    if c = invalid then return
    h = 40
    pad = 20
    fsize = 15
    if m.top.small
        h = 30
        pad = 15
        fsize = 12
    end if
    x = 0
    for i = 0 to c.getChildCount() - 1
        item = c.getChild(i)
        g = m.strip.createChild("Group")
        bg = g.createChild("Poster")
        ring = g.createChild("Poster")
        if m.top.small
            ring.uri = "pkg:/images/chip_focus.9.png"
        else
            ring.uri = "pkg:/images/pill_ring.9.png"
        end if
        ring.visible = false
        icon = invalid
        lx = pad
        if toS(item.HDPosterUrl) <> ""
            icon = g.createChild("Poster")
            icon.uri = item.HDPosterUrl
            icon.width = 18
            icon.height = 18
            icon.translation = [pad - 4, (h - 18) / 2]
            lx = pad + 20
        end if
        lb = g.createChild("Label")
        f = CreateObject("roSGNode", "Font")
        f.uri = "pkg:/fonts/Poppins-Bold.ttf"
        f.size = fsize
        lb.font = f
        lb.text = item.title
        lb.height = h
        lb.vertAlign = "center"
        lb.translation = [lx, 0]
        w = lx + lb.boundingRect().width + pad
        bg.width = w
        bg.height = h
        ring.width = w
        ring.height = h
        g.translation = [x, 0]
        g.scaleRotateCenter = [w / 2, h / 2]
        m.pills.Push({ g: g, bg: bg, ring: ring, lb: lb, w: w, x: x })
        x = x + w + 10
    end for
    if x > 0 then x = x - 10
    m.top.rowWidth = x
    if m.idx >= m.pills.Count() then m.idx = 0
    paint()
end sub

function toS(v as Dynamic) as String
    if v = invalid then return ""
    t = Type(v)
    if t = "String" or t = "roString" then return v
    return ""
end function

sub onFocus()
    paint()
end sub

sub onJump()
    j = m.top.jumpToItem
    if j >= 0 and j < m.pills.Count()
        m.idx = j
        m.top.itemFocused = j
        paint()
    end if
end sub

sub paint()
    focused = m.top.hasFocus()
    small = m.top.small
    for i = 0 to m.pills.Count() - 1
        p = m.pills[i]
        isOn = focused and i = m.idx
        pink = (i < m.top.primaryCount) or (i = m.top.selectedIndex) or isOn
        if small
            if pink then p.bg.uri = "pkg:/images/chip_pink.9.png" else p.bg.uri = "pkg:/images/chip_glass.9.png"
        else
            if pink then p.bg.uri = "pkg:/images/pill_pink.9.png" else p.bg.uri = "pkg:/images/pill_glass.9.png"
        end if
        p.ring.visible = isOn
        if isOn then p.g.scale = [1.06, 1.06] else p.g.scale = [1, 1]
        if pink or isOn then p.lb.color = "0xFFFFFFFF" else p.lb.color = "0xCDB8C9FF"
    end for
    ' Si la fila es más ancha que el espacio, se corre para que se vea el botón enfocado.
    if m.pills.Count() > 0 and m.top.rowWidth > m.top.maxWidth
        p = m.pills[m.idx]
        shift = 0
        if p.x + p.w > m.top.maxWidth then shift = m.top.maxWidth - (p.x + p.w)
        m.strip.translation = [shift, 0]
    else
        m.strip.translation = [0, 0]
    end if
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "left"
        if m.idx > 0
            m.idx = m.idx - 1
            m.top.itemFocused = m.idx
            paint()
            return true
        end if
        return false
    end if
    if key = "right"
        if m.idx < m.pills.Count() - 1
            m.idx = m.idx + 1
            m.top.itemFocused = m.idx
            paint()
            return true
        end if
        return true
    end if
    if key = "OK"
        if m.pills.Count() > 0 then m.top.itemSelected = m.idx
        return true
    end if
    return false
end function
