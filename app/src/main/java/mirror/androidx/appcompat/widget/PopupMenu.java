package mirror.androidx.appcompat.widget;

import com.canyie.dreamland.manager.utils.reflect.Reflection;

/**
 * Mirror class of {@link androidx.appcompat.widget.PopupMenu}
 * @author canyie
 * @date 2019/12/31.
 */
@SuppressWarnings("unchecked")
public final class PopupMenu {
    public static final String NAME = "androidx.appcompat.widget.PopupMenu";
    public static final Reflection<androidx.appcompat.widget.PopupMenu> REF = (Reflection<androidx.appcompat.widget.PopupMenu>) Reflection.on(NAME);
    public static final Reflection.FieldWrapper mPopup = REF.field("mPopup");

    private PopupMenu() {
        throw new InstantiationError("Mirror class mirror.androidx.appcompat.widget.PopupMenu");
    }
}
