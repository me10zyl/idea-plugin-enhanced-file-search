import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.ProjectViewImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ScrollToSource {

    public void scrollToSource(Project project, VirtualFile selectedFile)  {
        try {
            ProjectViewImpl projectView = (ProjectViewImpl) ProjectView.getInstance(project);

            Class<ProjectViewImpl> clazz = ProjectViewImpl.class;

            Field[] fields = clazz.getDeclaredFields();
            Field myAutoScrollFromSourceHandlerField = null;
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if ("MyAutoScrollFromSourceHandler".equals(field.getType().getSimpleName())) {
                    myAutoScrollFromSourceHandlerField = field;
                }
            }

            myAutoScrollFromSourceHandlerField.setAccessible(true);
            Object handler = myAutoScrollFromSourceHandlerField.get(projectView);

            Class<?>[] clazzes = clazz.getDeclaredClasses();
            for (int i = 0; i < clazzes.length; i++) {
                Class<?> clazze = clazzes[i];
                String simpleName = clazze.getSimpleName();
                if ("MyAutoScrollFromSourceHandler".equals(simpleName)) {
                    Method[] methods = clazze.getDeclaredMethods();
                    Method fromSource = null;
                    for (Method method : methods) {
                        if(method.getName().equals("scrollFromFile")){
                            fromSource = method;
                        }
                    }
                    fromSource.setAccessible(true);
                    PsiFile file = PsiManager.getInstance(project).findFile(selectedFile);
                    if (file != null) {
                        fromSource.invoke(handler, file, null);
                    }
                    return;
                }
            }
        }catch (IllegalAccessException e){
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
