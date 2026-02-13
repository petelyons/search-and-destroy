#!/usr/bin/env ruby

# Script to remove underscore prefixes from Java class fields
# and ensure all references use 'this.' instead

require 'set'

class JavaFieldRefactor
  def initialize(file_path)
    @file_path = file_path
    @content = File.read(file_path)
    @lines = @content.lines
    @fields = {}  # Maps _fieldName => fieldName
  end

  def refactor!
    find_underscore_fields
    return if @fields.empty?

    puts "Processing #{@file_path}..."
    puts "  Found #{@fields.size} underscore-prefixed fields: #{@fields.keys.join(', ')}"

    replace_field_declarations
    replace_field_references

    File.write(@file_path, @lines.join)
    puts "  ✓ Refactored #{@fields.size} fields"
  end

  private

  def find_underscore_fields
    @lines.each do |line|
      # Match field declarations like: private Type _fieldName; or private Type _fieldName[][];
      # Handles various modifiers and generics
      if line =~ /^\s*(private|protected|public|static|final|\s)+\s+[\w<>\[\],\s]+\s+(_\w+)(\[\])*\s*[;=]/
        field_with_underscore = $2
        field_without_underscore = field_with_underscore[1..-1]  # Remove leading _
        @fields[field_with_underscore] = field_without_underscore
      end
    end
  end

  def replace_field_declarations
    @lines.map! do |line|
      modified_line = line
      @fields.each do |old_name, new_name|
        # Replace field declarations (handles arrays like _field[][] -> field[][])
        modified_line = modified_line.gsub(/\b#{Regexp.escape(old_name)}\b(?=(\[\])*\s*[;=])/, new_name)
      end
      modified_line
    end
  end

  def replace_field_references
    @lines.map! do |line|
      modified_line = line

      @fields.each do |old_name, new_name|
        # Skip if this is a field declaration line (already handled)
        next if line =~ /^\s*(private|protected|public|static|final|\s)+.*\b#{Regexp.escape(old_name)}\b(\[\])*\s*[;=]/

        # Replace _field with this.field when not already prefixed with this.
        # Handles: _field, _field.method(), _field[index], _field.property
        modified_line = modified_line.gsub(/(?<!this\.)(?<!\.)\b#{Regexp.escape(old_name)}\b/) do |match|
          # Check if we're in a string literal (basic check)
          before_match = $`
          quote_count = before_match.scan(/"/).length

          if quote_count.odd?
            # We're inside a string, don't replace
            match
          else
            "this.#{new_name}"
          end
        end
      end

      modified_line
    end
  end
end

# Find all Java files
def find_java_files(directory = '.')
  Dir.glob("#{directory}/**/*.java")
end

# Main execution
if __FILE__ == $0
  puts "Java Underscore Field Refactoring Script"
  puts "=" * 50

  java_files = find_java_files('src')

  puts "Found #{java_files.length} Java files"
  puts

  total_refactored = 0

  java_files.each do |file|
    refactor = JavaFieldRefactor.new(file)
    refactor.refactor!
  rescue => e
    puts "ERROR processing #{file}: #{e.message}"
    puts e.backtrace.first(3)
  end

  puts
  puts "=" * 50
  puts "Refactoring complete!"
end
