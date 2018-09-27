import com.intellij.codeInsight.lookup.impl.LookupActionsStep;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.ui.RowIcon;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class SearchAction extends AnAction {

    public ScrollToSource scrollToSource = new ScrollToSource();

    private static VirtualFile getCurrentFile(AnActionEvent e) {
        return PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
    }

    private static Project getProject(AnActionEvent e) {
        return PlatformDataKeys.PROJECT.getData(e.getDataContext());
    }

    private static Module getModule(AnActionEvent e) {
        return LangDataKeys.MODULE.getData(e.getDataContext());
    }

    private static Editor getEditor(AnActionEvent e) {
        return PlatformDataKeys.EDITOR.getData(e.getDataContext());
    }

    private static ListPopupStep buildStep(@NotNull final List<Pair<AnAction, KeyStroke>> actions, final DataContext ctx) {
        return new BaseListPopupStep<Pair<AnAction, KeyStroke>>("Choose an action", ContainerUtil.findAll(actions, new Condition<Pair<AnAction, KeyStroke>>() {
            @Override
            public boolean value(Pair<AnAction, KeyStroke> pair) {
                final AnAction action = pair.getFirst();
                final Presentation presentation = action.getTemplatePresentation().clone();
                AnActionEvent event = new AnActionEvent(null, ctx,
                        ActionPlaces.UNKNOWN,
                        presentation,
                        ActionManager.getInstance(),
                        0);

                ActionUtil.performDumbAwareUpdate(action, event, true);
                return presentation.isEnabled() && presentation.isVisible();
            }
        })) {
            @Override
            public PopupStep onChosen(Pair<AnAction, KeyStroke> selectedValue, boolean finalChoice) {
                return FINAL_CHOICE;
            }
        };
    }

    public void walk( String path , List<File> files) {

        File root = new File( path );
        File[] list = root.listFiles();
        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                walk( f.getAbsolutePath(), files);
            }
            else {
                files.add(f);
            }
        }
    }

    private boolean isChild(String parentPath, String childPath){
        return childPath.startsWith(parentPath);
        /*
        List<File> fileList = new ArrayList<>();
        boolean isChild = false;
        walk(parentPath, fileList);
        for (File f : fileList){
            if(f.getAbsolutePath().equals(childPath)){
                isChild = true;
                break;
            }
        }*/
    }


    @Override
    public void actionPerformed(AnActionEvent e) {
        Module module = getModule(e);
        Project project = e.getProject();
        PsiManager psiManager = PsiManager.getInstance(project);
        JBPopupFactory pop = JBPopupFactory.getInstance();
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        List<VirtualFile> fileList = new ArrayList<>();
        @SystemIndependent String imlPath = module.getModuleFilePath();
        String parentPath = new File(imlPath).getParent();
        projectFileIndex.iterateContent(new ContentIterator() {
            @Override
            public boolean processFile(VirtualFile virtualFile) {
                Module moduleForFile = projectFileIndex.getModuleForFile(virtualFile);
                @SystemIndependent String modulePath = moduleForFile.getModuleFilePath();
                boolean isChild = isChild(parentPath, modulePath);
                if(module.equals(moduleForFile) || isChild){
                    if(virtualFile.isDirectory()){
                        VirtualFile[] children = virtualFile.getChildren();
                        boolean onlyDirectory = true;
                        for (VirtualFile child : children) {
                            if(!child.isDirectory()){
                                onlyDirectory = false;
                                break;
                            }
                        }
                        if(!onlyDirectory) {
                            fileList.add(virtualFile);
                        }
                    }
                }
                return true;
            }
        });
    /*
        Map<Icon, List<VirtualFile>> map = new HashMap<>();
        for (VirtualFile virtualFile : fileList) {
            PsiDirectory d = psiManager.findDirectory(virtualFile);
            Icon icon = d.getIcon(1);
            List<VirtualFile> virtualFiles = map.get(icon);
            if(virtualFiles == null){
                virtualFiles = new ArrayList<>();
                boolean put = false;
                for(Icon i: map.keySet()){
                    if(i.equals(icon)){
                        map.put(icon, virtualFiles);
                        put = true;
                        break;
                    }
                }
                if(!put) {
                    map.put(icon, virtualFiles);
                }
            }
            virtualFiles.add(virtualFile);
        }*/


        /*for (Map.Entry<Icon, List<VirtualFile>> entry : map.entrySet()) {
            fileList.addAll(entry.getValue());
        }*/

        /*fileList.sort(new Comparator<VirtualFile>() {
            @Override
            public int compare(VirtualFile o1, VirtualFile o2) {
                PsiDirectory d1 = psiManager.findDirectory(o1);
                PsiDirectory d2 = psiManager.findDirectory(o2);
                if(d1.getIcon(1).equals(d2.getIcon(1))){
                    return 1;
                }
                return 0;

            }
        });*/
        ListPopupStep step = new BaseListPopupStep<VirtualFile>("Project Directories", fileList){

            @NotNull
            @Override
            public String getTextFor(VirtualFile value) {
                return value.getPath();
            }

            @Override
            public Icon getIconFor(VirtualFile value) {
                PsiDirectory directory = psiManager.findDirectory(value);
                if(directory instanceof PsiJavaDirectoryImpl){
                    ItemPresentation presentation = ((PsiJavaDirectoryImpl) directory).getPresentation();
                    String s ="..";
                }
                return directory.getIcon(1);
            }

            @Override
            public boolean isMnemonicsNavigationEnabled() {
                return true;
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public String getIndexedString(VirtualFile value) {
                return value.getPath();
            }

            @Override
            public PopupStep onChosen(VirtualFile selectedValue, boolean finalChoice) {
                VirtualFile[] children = selectedValue.getChildren();
                if(children.length > 0) {
                    VirtualFile firstFile = children[0];
                    for (int i = 0; i < children.length;i++) {
                        VirtualFile child = children[i];
                        if(!child.isDirectory()){
                            firstFile = child;
                            break;
                        }
                    }
                    scrollToSource.scrollToSource(project, firstFile);
                }
                return FINAL_CHOICE;
            }
        };
        JBPopup popup = pop.createListPopup(step);
        Component focused = WindowManagerEx.getInstanceEx().getFocusedComponent(project);
       // popup.show(focused);
        JComponent component = (JComponent) focused;
        Rectangle visibleBounds = component != null ? component.getVisibleRect() : new Rectangle(component.getSize());
        Point containerScreenPoint = visibleBounds.getLocation();
        SwingUtilities.convertPointToScreen(containerScreenPoint, component);
        visibleBounds.setLocation(containerScreenPoint);
        RelativePoint relativePoint = new RelativePoint(containerScreenPoint);
        popup.show(relativePoint);
    }
}
