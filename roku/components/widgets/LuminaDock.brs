' Barra de menú flotante. Izquierda / derecha recorre las opciones; OK avisa cuál se eligió en "selected".

sub init()
    m.defs = [
        { id: "search", label: "Buscar", icon: "ic_search" }
        { id: "home", label: "Inicio", icon: "ic_home" }
        { id: "live", label: "En vivo", icon: "ic_live" }
        { id: "movie", label: "Películas", icon: "ic_movies" }
        { id: "series", label: "Series", icon: "ic_series" }
        { id: "mine", label: "Mi lista", icon: "ic_list" }
        { id: "settings", label: "Ajustes", icon: "ic_settings" }
    ]
    m.items = m.top.findNode("items")
    m.nodes = []
    x = 0
    for each d in m.defs
        g = m.items.createChild("Group")
        g.translation = [x, 0]
        act = g.createChild("Poster")
        act.uri = "pkg:/images/dock_active.9.png"
        act.width = 64
        act.height = 52
        act.visible = false
        on = g.createChild("Poster")
        on.uri = "pkg:/images/dock_on.9.png"
        on.width = 64
        on.height = 52
        on.visible = false
        ic = g.createChild("Poster")
        ic.uri = "pkg:/images/" + d.icon + ".png"
        ic.width = 22
        ic.height = 22
        ic.translation = [21, 7]
        lb = g.createChild("Label")
        lb.text = d.label
        lb.width = 64
        lb.height = 16
        lb.translation = [0, 31]
        lb.horizAlign = "center"
        f = CreateObject("roSGNode", "Font")
        f.uri = "pkg:/fonts/Poppins-Medium.ttf"
        f.size = 10
        lb.font = f
        m.nodes.Push({ act: act, on: on, ic: ic, lb: lb })
        x = x + 71
    end for
    m.idx = 1
    m.activeIdx = 1
    m.top.observeField("focusedChild", "onFocus")
    paint()
end sub

sub onActive()
    for i = 0 to m.defs.Count() - 1
        if m.defs[i].id = m.top.active
            m.activeIdx = i
            m.idx = i
        end if
    end for
    paint()
end sub

sub onFocus()
    paint()
end sub

sub paint()
    focused = m.top.hasFocus()
    for i = 0 to m.nodes.Count() - 1
        n = m.nodes[i]
        isOn = focused and i = m.idx
        isAct = (i = m.activeIdx)
        n.on.visible = isOn
        n.act.visible = isAct and not isOn
        if isOn
            n.ic.blendColor = "0xFFFFFFFF"
            n.lb.color = "0xFFFFFFFF"
        else if isAct
            n.ic.blendColor = "0xFF9ED0FF"
            n.lb.color = "0xFF9ED0FF"
        else
            n.ic.blendColor = "0xB9A2B5FF"
            n.lb.color = "0xB9A2B5FF"
        end if
    end for
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "left"
        if m.idx > 0 then m.idx = m.idx - 1
        paint()
        return true
    end if
    if key = "right"
        if m.idx < m.defs.Count() - 1 then m.idx = m.idx + 1
        paint()
        return true
    end if
    if key = "OK"
        m.top.selected = m.defs[m.idx].id
        return true
    end if
    return false
end function
