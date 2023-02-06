package com.tyron.completion.psi.completion.item;

import androidx.annotation.NonNull;

import com.tyron.completion.TailType;
import com.tyron.completion.lookup.LookupElement;
import com.tyron.completion.lookup.LookupElementPresentation;
import com.tyron.completion.lookup.impl.DefaultLookupItemRenderer;
import com.tyron.completion.lookup.impl.LookupItem;
import com.tyron.completion.psi.completion.AllClassesGetter;
import com.tyron.completion.psi.completion.TypedLookupItem;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Comparing;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.com.intellij.psi.JavaPsiFacade;
import org.jetbrains.kotlin.com.intellij.psi.PsiClass;
import org.jetbrains.kotlin.com.intellij.psi.PsiClassType;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiMethod;
import org.jetbrains.kotlin.com.intellij.psi.PsiModifier;
import org.jetbrains.kotlin.com.intellij.psi.PsiSubstitutor;
import org.jetbrains.kotlin.com.intellij.psi.PsiType;
import org.jetbrains.kotlin.com.intellij.psi.PsiTypeParameter;
import org.jetbrains.kotlin.com.intellij.psi.SmartPointerManager;
import org.jetbrains.kotlin.com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.resolve.JavaResolveUtil;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.kotlin.com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.Set;

public class JavaPsiClassReferenceElement extends LookupItem<Object> implements TypedLookupItem {

    private final SmartPsiElementPointer<PsiClass> myClass;
    private final String myQualifiedName;
    private String myForcedPresentableName;
    private final String myPackageDisplayName;
    private PsiSubstitutor mySubstitutor = PsiSubstitutor.EMPTY;

    public JavaPsiClassReferenceElement(PsiClass psiClass) {
        super(psiClass.getName(), psiClass.getName());
        myQualifiedName = psiClass.getQualifiedName();
        myClass = SmartPointerManager.getInstance(psiClass.getProject()).createSmartPsiElementPointer(psiClass);
        setInsertHandler(AllClassesGetter.TRY_SHORTENING);
        setTailType(TailType.NONE);
        myPackageDisplayName = PsiFormatUtil.getPackageDisplayName(psiClass);
    }

    public String getForcedPresentableName() {
        return myForcedPresentableName;
    }

    @Nullable
    @Override
    public PsiType getType() {
        PsiClass psiClass = getObject();
        return JavaPsiFacade.getElementFactory(psiClass.getProject()).createType(psiClass, getSubstitutor());
    }

    public PsiSubstitutor getSubstitutor() {
        return mySubstitutor;
    }

    public JavaPsiClassReferenceElement setSubstitutor(PsiSubstitutor substitutor) {
        mySubstitutor = substitutor;
        return this;
    }

    @NotNull
    @Override
    public String getLookupString() {
        if (myForcedPresentableName != null) {
            return myForcedPresentableName;
        }
        return super.getLookupString();
    }

    @Override
    public Set<String> getAllLookupStrings() {
        if (myForcedPresentableName != null) {
            return Collections.singleton(myForcedPresentableName);
        }

        return super.getAllLookupStrings();
    }

    public void setForcedPresentableName(String forcedPresentableName) {
        myForcedPresentableName = forcedPresentableName;
    }

    @NotNull
    @Override
    public PsiClass getObject() {
        PsiClass element = myClass.getElement();
        if (element == null) throw new IllegalStateException("Cannot restore from " + myClass);
        return element;
    }

    @Override
    public boolean isValid() {
        return myClass.getElement() != null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaPsiClassReferenceElement)) return false;

        JavaPsiClassReferenceElement that = (JavaPsiClassReferenceElement) o;
        if (myQualifiedName != null) {
            return myQualifiedName.equals(that.myQualifiedName);
        }
        return Comparing.equal(myClass, that.myClass);
    }

    public String getQualifiedName() {
        return myQualifiedName;
    }

    @Override
    public int hashCode() {
        final String s = myQualifiedName;
        return s == null ? 239 : s.hashCode();
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        renderClassItem(presentation, this, getObject(), false, " " + myPackageDisplayName, mySubstitutor);
    }

    public static void renderClassItem(LookupElementPresentation presentation, LookupElement item, PsiClass psiClass, boolean diamond,
                                       @NotNull String locationString, @NotNull PsiSubstitutor substitutor) {
        if (!(psiClass instanceof PsiTypeParameter)) {
//            presentation.setIcon(DefaultLookupItemRenderer.getRawIcon(item));
        }

        boolean strikeout = JavaElementLookupRenderer.isToStrikeout(item);
        presentation.setItemText(getName(psiClass, item, diamond, substitutor));
        presentation.setStrikeout(strikeout);

        String tailText = locationString;

//        if (item instanceof PsiTypeLookupItem) {
//            if (((PsiTypeLookupItem)item).isIndicateAnonymous() &&
//                (psiClass.isInterface() || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) ||
//                ((PsiTypeLookupItem)item).isAddArrayInitializer()) {
//                tailText = "{...}" + tailText;
//            }
//        }
        if (substitutor == PsiSubstitutor.EMPTY && !diamond && psiClass.getTypeParameters().length > 0) {
            String separator = "," + (showSpaceAfterComma(psiClass) ? " " : "");
            tailText = "<" + StringUtil.join(psiClass.getTypeParameters(), PsiTypeParameter::getName, separator) + ">" + tailText;
        }
        presentation.setTailText(tailText, true);
    }

    public String getLocationString() {
        return " " + myPackageDisplayName;
    }

    private static String getName(final PsiClass psiClass, final LookupElement item, boolean diamond, @NotNull PsiSubstitutor substitutor) {
        String forced = item instanceof JavaPsiClassReferenceElement ? ((JavaPsiClassReferenceElement)item).getForcedPresentableName() : null;
//                item instanceof PsiTypeLookupItem ? ((PsiTypeLookupItem)item).getForcedPresentableName() :
//                        null;
        if (forced != null) {
            return forced;
        }

        String name = PsiUtilCore.getName(psiClass);
        if (diamond) {
            return name + "<>";
        }

        if (substitutor != PsiSubstitutor.EMPTY) {
            final PsiTypeParameter[] params = psiClass.getTypeParameters();
            if (params.length > 0) {
                return name + formatTypeParameters(substitutor, params);
            }
        }

        return StringUtil.notNullize(name);
    }

    @NotNull
    private static String formatTypeParameters(@NotNull final PsiSubstitutor substitutor, final PsiTypeParameter[] params) {
        final boolean space = showSpaceAfterComma(params[0]);
        StringBuilder buffer = new StringBuilder();
        buffer.append("<");
        for(int i = 0; i < params.length; i++){
            final PsiTypeParameter param = params[i];
            final PsiType type = substitutor.substitute(param);
            if(type == null){
                return "";
            }
            if (type instanceof PsiClassType && ((PsiClassType)type).getParameters().length > 0) {
                buffer.append(((PsiClassType)type).rawType().getPresentableText()).append("<...>");
            } else {
                buffer.append(type.getPresentableText());
            }

            if(i < params.length - 1) {
                buffer.append(",");
                if (space) {
                    buffer.append(" ");
                }
            }
        }
        buffer.append(">");
        return buffer.toString();
    }

    private static boolean showSpaceAfterComma(PsiClass element) {
//        return CodeStyle.getLanguageSettings(element.getContainingFile(), JavaLanguage.INSTANCE).SPACE_AFTER_COMMA;
        return true;
    }

    static boolean isInaccessibleConstructorSuggestion(@NotNull PsiElement position, @Nullable PsiClass cls) {
        if (cls == null || cls.hasModifierProperty(PsiModifier.ABSTRACT)) return false;
        PsiMethod[] constructors = cls.getConstructors();
        if (constructors.length > 0) {
            return !ContainerUtil.exists(constructors, ctor ->
                    JavaResolveUtil.isAccessible(ctor, cls, ctor.getModifierList(), position, null, null));
        }
        return false;
    }
}