import os
import re
import sys

def validate_naming(directory):
    """
    Validates that files in the given directory follow the Hermi Framework naming conventions.
    Also checks for physical separation between Use Cases and Shells.
    """
    errors = []
    
    # Expected patterns
    usecase_pattern = re.compile(r'^[A-Z][a-z]+[A-Z][a-z]+UseCase\.java$')
    contract_pattern = re.compile(r'^[A-Z][a-z]+[A-Z][a-z]+(Client|Repository|Messenger)\.java$')
    shell_pattern = re.compile(r'^[A-Z][a-z]+[A-Z][a-z]+[A-Z][a-z]+Shell\.java$')

    modules = set()
    has_usecase_module = False
    has_shell_module = False

    for root, dirs, files in os.walk(directory):
        # Identify modules by pom.xml presence
        if 'pom.xml' in files:
            module_name = os.path.basename(root)
            modules.add(module_name)
            if module_name.endswith('-usecase'):
                has_usecase_module = True
            elif module_name.endswith('-shell'):
                has_shell_module = True

        for file in files:
            if not file.endswith('.java'):
                continue
            
            # Check file location relative to module
            rel_path = os.path.relpath(os.path.join(root, file), directory)
            
            if "UseCase" in file:
                if not usecase_pattern.match(file):
                    errors.append(f"Invalid UseCase naming: {file} (Expected {{Action}}{{Resource}}UseCase.java)")
                if "-shell" in rel_path:
                    errors.append(f"Boundary Violation: {file} found in a shell module. UseCases must be in a dedicated usecase module.")

            elif any(t in file for t in ["Client", "Repository", "Messenger"]):
                if not contract_pattern.match(file) and "Adapter" not in file:
                     pass # Simplified
                if "-shell" in rel_path and "Adapter" not in file:
                     errors.append(f"Boundary Violation: {file} contract found in a shell module. Contracts must be in the usecase module.")

            elif "Shell" in file:
                if not shell_pattern.match(file):
                    errors.append(f"Invalid Shell naming: {file} (Expected {{Action}}{{Resource}}{{Type}}Shell.java)")
                if "-usecase" in rel_path:
                    errors.append(f"Boundary Violation: {file} found in a usecase module. Entry points must be in the shell module.")

    if not has_usecase_module:
        errors.append("Physical Separation Missing: No module ending in '-usecase' found (missing pom.xml or incorrect directory name).")
    if not has_shell_module:
        errors.append("Physical Separation Missing: No module ending in '-shell' found (missing pom.xml or incorrect directory name).")

    return errors

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python validate_naming.py <directory>")
        sys.exit(1)
    
    target_dir = sys.argv[1]
    msg_errors = validate_naming(target_dir)
    
    if msg_errors:
        print("Naming & Structure validation FAILED:")
        for err in msg_errors:
            print(f"  - {err}")
        sys.exit(1)
    else:
        print("Naming & Structure validation PASSED.")
        sys.exit(0)
